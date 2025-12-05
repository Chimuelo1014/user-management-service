package com.sentinel.user_management_service.dto.response;

import com.sentinel.user_management_service.enums.InvitationStatus;
import com.sentinel.user_management_service.enums.InvitationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    
    // ✅ NUEVO: Lista de proyectos
    @Builder.Default
    private List<UUID> projectIds = new ArrayList<>();
    
    // ✅ NUEVO: Información de proyectos
    @Builder.Default
    private List<ProjectInfo> projects = new ArrayList<>();
    
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectInfo {
        private UUID id;
        private String name;
    }
}