package com.sentinel.user_management_service.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardDTO {
    
    private UUID userId;
    private String email;
    
    // Plan del usuario
    private UserPlanDTO plan;
    
    // Tenants donde es miembro (con rol incluido)
    private List<TenantWithRoleDTO> memberTenants;
    
    // Proyectos donde es miembro (con rol incluido)
    private List<ProjectWithDetailsDTO> memberProjects;
    
    // Invitaciones pendientes
    private List<InvitationDTO> pendingInvitations;
}