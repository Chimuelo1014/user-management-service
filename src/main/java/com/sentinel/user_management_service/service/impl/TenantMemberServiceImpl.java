package com.sentinel.user_management_service.service.impl;

import com.sentinel.user_management_service.dto.response.TenantMemberDTO;
import com.sentinel.user_management_service.entity.TenantMemberEntity;
import com.sentinel.user_management_service.enums.TenantRole;
import com.sentinel.user_management_service.exception.MemberAlreadyExistsException;
import com.sentinel.user_management_service.exception.MemberNotFoundException;
import com.sentinel.user_management_service.exception.PermissionDeniedException;
import com.sentinel.user_management_service.repository.TenantMemberRepository;
import com.sentinel.user_management_service.service.TenantMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMemberServiceImpl implements TenantMemberService {

    private final TenantMemberRepository tenantMemberRepository;

    @Override
    @Transactional
    public TenantMemberDTO addMember(UUID tenantId, UUID userId, TenantRole role, UUID invitedBy) {
        log.info("Adding member {} to tenant {} with role {}", userId, tenantId, role);

        if (tenantMemberRepository.existsByTenantIdAndUserId(tenantId, userId)) {
            throw new MemberAlreadyExistsException("User is already a member of this tenant");
        }

        TenantMemberEntity member = TenantMemberEntity.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .invitedBy(invitedBy)
                .build();

        tenantMemberRepository.save(member);

        log.info("Member added successfully: {}", member.getId());

        return mapToDTO(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMemberDTO> getTenantMembers(UUID tenantId) {
        return tenantMemberRepository.findByTenantId(tenantId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMemberDTO> getUserTenants(UUID userId) {
        return tenantMemberRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantMemberDTO getMemberRole(UUID tenantId, UUID userId) {
        TenantMemberEntity member = tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new MemberNotFoundException("User is not a member of this tenant"));

        return mapToDTO(member);
    }

    @Override
    @Transactional
    public void removeMember(UUID tenantId, UUID userId, UUID requestingUserId) {
        log.info("Removing member {} from tenant {} by user {}", userId, tenantId, requestingUserId);

        // Verify requesting user is admin
        TenantMemberEntity requestingMember = tenantMemberRepository.findByTenantIdAndUserId(tenantId, requestingUserId)
                .orElseThrow(() -> new PermissionDeniedException("You are not a member of this tenant"));

        if (!requestingMember.isAdmin()) {
            throw new PermissionDeniedException("Only admins can remove members");
        }

        // Verify target member exists
        if (!tenantMemberRepository.existsByTenantIdAndUserId(tenantId, userId)) {
            throw new MemberNotFoundException("User is not a member of this tenant");
        }

        // Count admins
        long adminCount = tenantMemberRepository.countByTenantIdAndRole(tenantId, TenantRole.TENANT_ADMIN);

        // Don't allow removing last admin
        TenantMemberEntity targetMember = tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId).get();
        if (targetMember.isAdmin() && adminCount <= 1) {
            throw new PermissionDeniedException("Cannot remove last admin from tenant");
        }

        tenantMemberRepository.deleteByTenantIdAndUserId(tenantId, userId);

        log.info("Member removed successfully");
    }

    @Override
    @Transactional
    public TenantMemberDTO updateMemberRole(UUID tenantId, UUID userId, TenantRole newRole, UUID requestingUserId) {
        log.info("Updating member {} role to {} in tenant {}", userId, newRole, tenantId);

        // Verify requesting user is admin
        TenantMemberEntity requestingMember = tenantMemberRepository.findByTenantIdAndUserId(tenantId, requestingUserId)
                .orElseThrow(() -> new PermissionDeniedException("You are not a member of this tenant"));

        if (!requestingMember.isAdmin()) {
            throw new PermissionDeniedException("Only admins can update member roles");
        }

        // Get target member
        TenantMemberEntity targetMember = tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new MemberNotFoundException("User is not a member of this tenant"));

        // Don't allow removing admin from last admin
        if (targetMember.isAdmin() && newRole != TenantRole.TENANT_ADMIN) {
            long adminCount = tenantMemberRepository.countByTenantIdAndRole(tenantId, TenantRole.TENANT_ADMIN);
            if (adminCount <= 1) {
                throw new PermissionDeniedException("Cannot demote last admin");
            }
        }

        targetMember.setRole(newRole);
        tenantMemberRepository.save(targetMember);

        log.info("Member role updated successfully");

        return mapToDTO(targetMember);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(UUID tenantId, UUID userId) {
        return tenantMemberRepository.existsByTenantIdAndUserId(tenantId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(UUID tenantId, UUID userId) {
        return tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .map(TenantMemberEntity::isAdmin)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMembers(UUID tenantId) {
        return tenantMemberRepository.countByTenantId(tenantId);
    }

    // Helper method
    private TenantMemberDTO mapToDTO(TenantMemberEntity entity) {
        return TenantMemberDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .userId(entity.getUserId())
                .role(entity.getRole())
                .joinedAt(entity.getJoinedAt())
                .invitedBy(entity.getInvitedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
