package com.sentinel.user_management_service.service;

import com.sentinel.user_management_service.dto.request.InviteUserRequest;
import com.sentinel.user_management_service.dto.response.InvitationDTO;
import com.sentinel.user_management_service.enums.InvitationType;

import java.util.List;
import java.util.UUID;

public interface InvitationService {
    
    InvitationDTO inviteUser(InviteUserRequest request, UUID invitedBy, String inviterEmail);
    
    void acceptInvitation(String token, UUID userId);
    
    void revokeInvitation(UUID invitationId, UUID requestingUserId);
    
    List<InvitationDTO> getPendingInvitations(UUID resourceId, InvitationType type);
    
    List<InvitationDTO> getUserInvitations(String email);
    
    InvitationDTO getInvitationByToken(String token);
    
    void cleanupExpiredInvitations();
}