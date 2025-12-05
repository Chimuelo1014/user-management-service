package com.sentinel.user_management_service.service.impl;

import com.sentinel.user_management_service.client.ProjectServiceClient;
import com.sentinel.user_management_service.client.TenantServiceClient;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        log.info("📤 Inviting user {} to {} {} by user {} ({})", 
            request.getEmail(), 
            request.getType(), 
            request.getResourceId(),
            invitedBy,
            inviterEmail);

        // ✅ Validate required fields
        if (request.getType() == null) {
            log.error("❌ Type is null!");
            throw new IllegalArgumentException("Type is required");
        }
        
        if (request.getResourceId() == null) {
            log.error("❌ ResourceId is null!");
            throw new IllegalArgumentException("ResourceId is required");
        }

        // ✅ Check for pending invitation
        if (invitationRepository.existsByEmailAndResourceIdAndTypeAndStatus(
                request.getEmail(),
                request.getResourceId(),
                request.getType(),
                InvitationStatus.PENDING)) {
            throw new MemberAlreadyExistsException("User already has a pending invitation");
        }

        // ✅ Validate tenant/project exists
        if (request.getType() == InvitationType.TENANT) {
            try {
                tenantClient.getTenant(request.getResourceId());
            } catch (Exception e) {
                log.error("❌ Tenant not found: {}", request.getResourceId());
                throw new IllegalArgumentException("Tenant not found");
            }
        }

        // ✅ Validate projects exist (if provided)
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

        // ✅ Generate token
        String token = generateInvitationToken();

        // ✅ Create invitation
        InvitationEntity invitation = InvitationEntity.builder()
                .email(request.getEmail())
                .token(token)
                .type(request.getType())
                .resourceId(request.getResourceId())
                .resourceName(request.getResourceName())
                .role(request.getRole())
                .projectIds(request.getProjectIds() != null ? request.getProjectIds() : List.of())
                .status(InvitationStatus.PENDING)
                .invitedBy(invitedBy)
                .inviterEmail(inviterEmail)
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();

        invitationRepository.save(invitation);

        log.info("✅ Invitation created: {} with {} projects", invitation.getId(), invitation.getProjectIds().size());

        // ✅ Publish event
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
            
            // Check if already member
            if (!tenantMemberService.isMember(invitation.getResourceId(), userId)) {
                tenantMemberService.addMember(
                    invitation.getResourceId(), 
                    userId, 
                    role, 
                    invitation.getInvitedBy()
                );
                log.info("✅ User added to tenant as {}", role);
            }

            // ✅ Add user to projects (if specified)
            if (invitation.getProjectIds() != null && !invitation.getProjectIds().isEmpty()) {
                for (UUID projectId : invitation.getProjectIds()) {
                    try {
                        // Default role for projects: PROJECT_MEMBER
                        ProjectRole projectRole = ProjectRole.PROJECT_MEMBER;
                        
                        if (!projectMemberService.isMember(projectId, userId)) {
                            projectMemberService.addMember(
                                projectId,
                                userId,
                                invitation.getResourceId(), // tenantId
                                projectRole,
                                invitation.getInvitedBy()
                            );
                            log.info("✅ User added to project {} as {}", projectId, projectRole);
                        }
                    } catch (Exception e) {
                        log.error("❌ Failed to add user to project {}: {}", projectId, e.getMessage());
                    }
                }
            }
        }

        // ✅ Mark as accepted
        invitation.accept();
        invitationRepository.save(invitation);

        log.info("✅ Invitation accepted successfully");

        // ✅ Publish event
        eventPublisher.publishInvitationAccepted(invitation, userId);
    }

    @Override
    @Transactional
    public void revokeInvitation(UUID invitationId, UUID requestingUserId) {
        log.info("🚫 Revoking invitation: {}", invitationId);

        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        // Verify permissions (only inviter or tenant admin)
        if (!invitation.getInvitedBy().equals(requestingUserId)) {
            if (!tenantMemberService.isAdmin(invitation.getResourceId(), requestingUserId)) {
                throw new PermissionDeniedException("Only inviter or admin can revoke invitations");
            }
        }

        invitation.revoke();
        invitationRepository.save(invitation);

        log.info("✅ Invitation revoked successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getPendingInvitations(UUID resourceId, InvitationType type) {
        return invitationRepository.findByResourceIdAndTypeAndStatus(resourceId, type, InvitationStatus.PENDING)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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
                .orElseThrow(() -> new InvitationNotFoundException("Invalid invitation token"));

        if (invitation.isExpired() && invitation.getStatus() == InvitationStatus.PENDING) {
            invitation.markExpired();
            invitationRepository.save(invitation);
            throw new InvitationExpiredException("Invitation has expired");
        }

        return mapToDTO(invitation);
    }

    @Override
    @Transactional
    public void cleanupExpiredInvitations() {
        log.info("🧹 Cleaning up expired invitations");

        int updated = invitationRepository.markExpiredInvitations(
                InvitationStatus.PENDING,
                InvitationStatus.EXPIRED,
                LocalDateTime.now()
        );

        log.info("✅ Marked {} invitations as expired", updated);

        // Delete old invitations (>90 days)
        invitationRepository.deleteOldInvitations(
                InvitationStatus.EXPIRED,
                LocalDateTime.now().minusDays(90)
        );
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private String generateInvitationToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        
        return token.toString();
    }

    private InvitationDTO mapToDTO(InvitationEntity entity) {
        String invitationUrl = invitationBaseUrl + "?token=" + entity.getToken();

        // ✅ Fetch project names if projectIds exist
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