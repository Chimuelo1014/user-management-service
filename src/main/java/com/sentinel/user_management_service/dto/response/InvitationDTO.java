package com.sentinel.user_management_service.dto.response;

import com.sentinel.user_management_service.enums.InvitationStatus;
import com.sentinel.user_management_service.enums.InvitationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDTO {
    
    private UUID id;
    private String email;
    private String token;
    private InvitationType type;
    private UUID resourceId;
    private String resourceName;
    private String role;
    private InvitationStatus status;
    private UUID invitedBy;
    private String inviterEmail;
    private String invitationUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
}