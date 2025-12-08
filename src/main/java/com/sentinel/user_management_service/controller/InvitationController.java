package com.sentinel.user_management_service.controller;

import com.sentinel.user_management_service.dto.request.AcceptInvitationRequest;
import com.sentinel.user_management_service.dto.response.InvitationDTO;
import com.sentinel.user_management_service.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/accept")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        invitationService.acceptInvitation(request.getToken(), userId);
        return ResponseEntity.ok(Map.of("message", "Invitation accepted successfully"));
    }

    @GetMapping("/me")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<List<InvitationDTO>> getMyInvitations(
            @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(invitationService.getUserInvitations(email));
    }

    @GetMapping("/{token}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<InvitationDTO> getInvitation(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.getInvitationByToken(token));
    }

    @DeleteMapping("/{invitationId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable UUID invitationId,
            @RequestHeader("X-User-Id") UUID requestingUserId) {
        invitationService.revokeInvitation(invitationId, requestingUserId);
        return ResponseEntity.noContent().build();
    }
}