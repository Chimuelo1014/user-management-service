package com.sentinel.user_management_service.controller;

import com.sentinel.user_management_service.dto.response.UserDashboardDTO;
import com.sentinel.user_management_service.service.UserPlanService;
import com.sentinel.user_management_service.service.TenantMemberService;
import com.sentinel.user_management_service.service.ProjectMemberService;
import com.sentinel.user_management_service.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserPlanService userPlanService;
    private final TenantMemberService tenantMemberService;
    private final ProjectMemberService projectMemberService;
    private final InvitationService invitationService;

    /**
     * Get complete dashboard data
     * GET /api/dashboard/me
     */
    @GetMapping("/me")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDashboardDTO> getDashboard(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email) {
        log.debug("Fetching dashboard for user: {}", userId);

        try {
            var userPlan = userPlanService.getUserPlan(userId);
            var memberTenants = tenantMemberService.getUserTenantsWithRole(userId);
            var memberProjects = projectMemberService.getUserProjectsWithDetails(userId);
            var pendingInvitations = invitationService.getUserInvitations(email);

            UserDashboardDTO dashboard = UserDashboardDTO.builder()
                    .userId(userId)
                    .email(email)
                    .plan(userPlan)
                    .memberTenants(memberTenants)
                    .memberProjects(memberProjects)
                    .pendingInvitations(pendingInvitations)
                    .build();

            log.info("✅ Dashboard fetched successfully for user: {}", userId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.warn("⚠️ Error fetching dashboard for user {}: {}. Creating default plan.", userId, e.getMessage());

            try {
                // Si falla obtener plan, intenta crear uno por defecto
                userPlanService.createDefaultPlan(userId);
                var userPlan = userPlanService.getUserPlan(userId);
                var memberTenants = tenantMemberService.getUserTenantsWithRole(userId);
                var memberProjects = projectMemberService.getUserProjectsWithDetails(userId);
                var pendingInvitations = invitationService.getUserInvitations(email);

                UserDashboardDTO dashboard = UserDashboardDTO.builder()
                        .userId(userId)
                        .email(email)
                        .plan(userPlan)
                        .memberTenants(memberTenants)
                        .memberProjects(memberProjects)
                        .pendingInvitations(pendingInvitations)
                        .build();

                log.info("✅ Dashboard fetched successfully after creating default plan for user: {}", userId);
                return ResponseEntity.ok(dashboard);
            } catch (Exception ex) {
                log.error("❌ Failed to create default plan for user {}: {}", userId, ex.getMessage(), ex);
                throw ex;
            }
        }
    }
}