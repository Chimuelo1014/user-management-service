package com.sentinel.user_management_service.controller;

import com.sentinel.user_management_service.dto.request.InviteUserRequest;
import com.sentinel.user_management_service.dto.response.InvitationDTO;
import com.sentinel.user_management_service.dto.response.TenantMemberDTO;
import com.sentinel.user_management_service.enums.InvitationType;
import com.sentinel.user_management_service.service.InvitationService;
import com.sentinel.user_management_service.service.TenantMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/tenants/{tenantId}/members")
@RequiredArgsConstructor
public class TenantMemberController {

    private final TenantMemberService tenantMemberService;
    private final InvitationService invitationService;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<List<TenantMemberDTO>> getMembers(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantMemberService.getTenantMembers(tenantId));
    }

    @PostMapping("/invite")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<InvitationDTO> inviteMember(
            @PathVariable UUID tenantId,
            @Valid @RequestBody InviteUserRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String userEmail) {
        // Auto-complete required fields
        request.setType(InvitationType.TENANT);
        request.setResourceId(tenantId);

        // If resourceName not provided, use a default
        if (request.getResourceName() == null || request.getResourceName().isEmpty()) {
            request.setResourceName("Tenant Invitation");
        }

        log.info("Inviting user {} to tenant {} with role {}", request.getEmail(), tenantId, request.getRole());

        InvitationDTO invitation = invitationService.inviteUser(request, userId, userEmail);
        return ResponseEntity.ok(invitation);
    }

    @DeleteMapping("/{memberId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID tenantId,
            @PathVariable UUID memberId,
            @RequestHeader("X-User-Id") UUID requestingUserId) {
        tenantMemberService.removeMember(tenantId, memberId, requestingUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<List<InvitationDTO>> getPendingInvitations(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(invitationService.getPendingInvitations(tenantId, InvitationType.TENANT));
    }
}