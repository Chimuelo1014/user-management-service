package com.sentinel.user_management_service.service.impl;

import com.sentinel.user_management_service.client.ProjectServiceClient;
import com.sentinel.user_management_service.client.TenantServiceClient;
import com.sentinel.user_management_service.client.dto.TenantDTO;
import com.sentinel.user_management_service.dto.request.InviteUserRequest;
import com.sentinel.user_management_service.dto.response.InvitationDTO;
import com.sentinel.user_management_service.entity.InvitationEntity;
import com.sentinel.user_management_service.enums.InvitationStatus;
import com.sentinel.user_management_service.enums.InvitationType;
import com.sentinel.user_management_service.enums.ProjectRole;
import com.sentinel.user_management_service.enums.TenantRole;
import com.sentinel.user_management_service.events.UserManagementEventPublisher;
import com.sentinel.user_management_service.exception.*;
import com.sentinel.user_management_service.repository.InvitationRepository;
import com.sentinel.user_management_service.service.InvitationService;
import com.sentinel.user_management_service.service.ProjectMemberService;
import com.sentinel.user_management_service.service.TenantMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final TenantMemberService tenantMemberService;
    private final ProjectMemberService projectMemberService;
    private final UserManagementEventPublisher eventPublisher;
    private final TenantServiceClient tenantClient;
    private final ProjectServiceClient projectClient;

    @Value("${invitation.expiration.days:7}")
    private int expirationDays;

    @Value("${invitation.base.url:http://localhost:3000/invitations/accept}")
    private String invitationBaseUrl;

    @Override
    @Transactional
    public InvitationDTO inviteUser(InviteUserRequest request, UUID invitedBy, String inviterEmail) {
        log.info("✉️ Inviting user {} to {} {}", request.getEmail(), request.getType(), request.getResourceId());

        // ✅ 1. Verificar que el email no sea del invitador
        if (request.getEmail().equalsIgnoreCase(inviterEmail)) {
            throw new IllegalArgumentException("Cannot invite yourself");
        }

        // ✅ 2. Verificar invitación pendiente no exista
        boolean pendingExists = invitationRepository
                .findByEmailAndResourceIdAndTypeAndStatus(
                        request.getEmail(),
                        request.getResourceId(),
                        request.getType(),
                        InvitationStatus.PENDING)
                .isPresent();

        if (pendingExists) {
            throw new MemberAlreadyExistsException("User already has a pending invitation");
        }

        // ✅ 3. Validar que el recurso existe
        // ✅ 3. Validar que el recurso existe y validar LÍMITES
        if (request.getType() == InvitationType.TENANT) {
            try {
                TenantDTO tenant = tenantClient.getTenant(request.getResourceId());

                // Validar si cabe un usuario más
                int currentUsers = tenant.getUsage().getCurrentUsers();
                var limitResponse = tenantClient.validateLimit(request.getResourceId(), "USER", currentUsers + 1);

                if (!limitResponse.isAllowed()) {
                    log.warn("❌ Limit validation denied for tenant {}: {}", request.getResourceId(),
                            limitResponse.getMessage());
                    throw new IllegalStateException(
                            limitResponse.getMessage() + ". " + limitResponse.getUpgradePlanHint());
                }

            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                log.error("❌ Tenant check failed: {}", request.getResourceId());
                throw new IllegalArgumentException("Tenant not found or unavailable");
            }
        }

        // ✅ 4. Validar proyectos si se proporcionan
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            for (UUID projectId : request.getProjectIds()) {
                try {
                    projectClient.getProject(projectId);
                } catch (Exception e) {
                    log.error("❌ Project not found: {}", projectId);
                    throw new IllegalArgumentException("Project not found: " + projectId);
                }
            }
        }

        // ✅ 5. Crear invitación
        String token = generateInvitationToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expirationDays);

        InvitationEntity invitation = InvitationEntity.builder()
                .email(request.getEmail())
                .token(token)
                .type(request.getType())
                .resourceId(request.getResourceId())
                .resourceName(request.getResourceName())
                .role(request.getRole())
                .status(InvitationStatus.PENDING)
                .invitedBy(invitedBy)
                .inviterEmail(inviterEmail)
                .expiresAt(expiresAt)
                .projectIds(request.getProjectIds() != null ? request.getProjectIds() : new ArrayList<>())
                .build();

        invitationRepository.save(invitation);
        log.info("✅ Invitation created: {} with {} projects", invitation.getId(), invitation.getProjectIds().size());

        // ✅ 6. Publicar evento para notification-service
        eventPublisher.publishUserInvited(invitation);

        return mapToDTO(invitation);
    }

    @Override
    @Transactional
    public void acceptInvitation(String token, UUID userId) {
        log.info("✅ Accepting invitation with token: {} by user: {}", token, userId);

        // Find invitation
        InvitationEntity invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid invitation token"));

        // Verify status
        if (!invitation.isPending()) {
            if (invitation.isExpired()) {
                throw new InvitationExpiredException("Invitation has expired");
            }
            throw new IllegalStateException("Invitation is not pending");
        }

        // ✅ Add user to tenant
        if (invitation.getType() == InvitationType.TENANT) {
            TenantRole role = TenantRole.valueOf(invitation.getRole());

            if (!tenantMemberService.isMember(invitation.getResourceId(), userId)) {
                tenantMemberService.addMember(
                        invitation.getResourceId(),
                        userId,
                        role,
                        invitation.getInvitedBy());

                // Incrementar contador de usuarios en tenant-service
                try {
                    tenantClient.incrementResource(invitation.getResourceId(), "USER");
                } catch (Exception e) {
                    log.error("Failed to increment user count for tenant {}: {}", invitation.getResourceId(),
                            e.getMessage());
                    // No fallamos la transacción, pero loggeamos el error.
                    // En un sistema real, esto debería ser eventual consistent o tener
                    // compensación.
                }

                log.info("✅ User added to tenant as {}", role);
            }
        }

        // ✅ Add user to projects
        if (invitation.getProjectIds() != null && !invitation.getProjectIds().isEmpty()) {
            for (UUID projectId : invitation.getProjectIds()) {
                try {
                    ProjectRole projectRole = ProjectRole.PROJECT_MEMBER;

                    if (!projectMemberService.isMember(projectId, userId)) {
                        projectMemberService.addMember(
                                projectId,
                                userId,
                                invitation.getResourceId(), // tenantId
                                projectRole,
                                invitation.getInvitedBy());
                        log.info("✅ User added to project {} as {}", projectId, projectRole);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Could not add user to project {}: {}", projectId, e.getMessage());
                }
            }
        }

        // ✅ Mark invitation as accepted
        invitation.accept();
        invitationRepository.save(invitation);

        // ✅ Publish event
        eventPublisher.publishInvitationAccepted(invitation, userId);
        log.info("✅ Invitation accepted and event published");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getUserInvitations(String email) {
        return invitationRepository.findByEmailAndStatus(email, InvitationStatus.PENDING)
                .stream()
                .filter(inv -> !inv.isExpired())
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationDTO getInvitationByToken(String token) {
        InvitationEntity invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid token"));
        return mapToDTO(invitation);
    }

    @Override
    @Transactional
    public void cleanupExpiredInvitations() {
        log.info("🧹 Cleaning up expired invitations...");
        List<InvitationEntity> expiredInvitations = invitationRepository.findByStatus(InvitationStatus.PENDING)
                .stream()
                .filter(InvitationEntity::isExpired)
                .toList();

        expiredInvitations.forEach(inv -> {
            inv.markExpired();
            invitationRepository.save(inv);
        });

        log.info("✅ Cleaned up {} expired invitations", expiredInvitations.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getPendingInvitations(UUID resourceId, InvitationType type) {
        log.debug("Fetching pending invitations for resource: {} type: {}", resourceId, type);

        return invitationRepository.findByResourceIdAndTypeAndStatus(resourceId, type, InvitationStatus.PENDING)
                .stream()
                .filter(inv -> !inv.isExpired())
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeInvitation(UUID invitationId, UUID requestingUserId) {
        log.info("🗑️ Revoking invitation: {} by user: {}", invitationId, requestingUserId);

        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        // Solo el que envió la invitación o un admin puede revocarla
        if (!invitation.getInvitedBy().equals(requestingUserId)) {
            throw new IllegalArgumentException("Only the inviter can revoke this invitation");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);

        log.info("✅ Invitation revoked: {}", invitationId);
    }

    private String generateInvitationToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private InvitationDTO mapToDTO(InvitationEntity entity) {
        String invitationUrl = invitationBaseUrl + "?token=" + entity.getToken();

        List<InvitationDTO.ProjectInfo> projects = List.of();
        if (entity.getProjectIds() != null && !entity.getProjectIds().isEmpty()) {
            projects = entity.getProjectIds().stream()
                    .map(projectId -> {
                        try {
                            var project = projectClient.getProject(projectId);
                            return InvitationDTO.ProjectInfo.builder()
                                    .id(projectId)
                                    .name(project.getName())
                                    .build();
                        } catch (Exception e) {
                            log.warn("⚠️ Could not fetch project {}: {}", projectId, e.getMessage());
                            return InvitationDTO.ProjectInfo.builder()
                                    .id(projectId)
                                    .name("Unknown Project")
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());
        }

        return InvitationDTO.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .token(entity.getToken())
                .type(entity.getType())
                .resourceId(entity.getResourceId())
                .resourceName(entity.getResourceName())
                .role(entity.getRole())
                .status(entity.getStatus())
                .invitedBy(entity.getInvitedBy())
                .inviterEmail(entity.getInviterEmail())
                .invitationUrl(invitationUrl)
                .projectIds(entity.getProjectIds())
                .projects(projects)
                .expiresAt(entity.getExpiresAt())
                .acceptedAt(entity.getAcceptedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}