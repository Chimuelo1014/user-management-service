package com.sentinel.user_management_service.controller;

import com.sentinel.user_management_service.dto.request.CheckPermissionRequest;
import com.sentinel.user_management_service.dto.response.PermissionCheckResponse;
import com.sentinel.user_management_service.dto.response.ProjectMemberDTO;
import com.sentinel.user_management_service.service.PermissionService;
import com.sentinel.user_management_service.service.ProjectMemberService;
import com.sentinel.user_management_service.service.TenantMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final PermissionService permissionService;
    private final TenantMemberService tenantMemberService;
    private final ProjectMemberService projectMemberService;

    @PostMapping("/permissions/check")
    public ResponseEntity<PermissionCheckResponse> checkPermission(
            @Valid @RequestBody CheckPermissionRequest request) {
        boolean hasPermission = permissionService.checkPermission(
                request.getUserId(),
                request.getTenantId(),
                request.getProjectId(),
                request.getPermission());
        
        return ResponseEntity.ok(PermissionCheckResponse.builder()
                .allowed(hasPermission)
                .userId(request.getUserId())
                .tenantId(request.getTenantId())
                .projectId(request.getProjectId())
                .permission(request.getPermission())
                .build());
    }

    @GetMapping("/permissions/tenant/{tenantId}/user/{userId}/role")
    public ResponseEntity<String> getTenantRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId) {
        log.debug("Internal: Getting tenant role for user {} in tenant {}", userId, tenantId);

        try {
            var member = tenantMemberService.getMemberRole(tenantId, userId);
            return ResponseEntity.ok(member.getRole().name());
        } catch (Exception e) {
            log.debug("User {} is not a member of tenant {}", userId, tenantId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener lista de tenants donde el usuario es miembro
     * GET /api/internal/users/{userId}/tenants
     */
    @GetMapping("/users/{userId}/tenants")
    public ResponseEntity<List<UUID>> getUserTenants(@PathVariable UUID userId) {
        log.debug("🔍 Internal: Fetching tenants for user: {}", userId);

        List<UUID> tenantIds = tenantMemberService.getUserTenantIds(userId);

        log.debug("✅ User {} is member of {} tenants: {}",
                userId, tenantIds.size(), tenantIds);

        return ResponseEntity.ok(tenantIds);
    }

    /**
     * ✅ NUEVO ENDPOINT: Obtener proyectos del usuario con información detallada
     * GET /api/internal/users/{userId}/projects
     */
    @GetMapping("/users/{userId}/projects")
    public ResponseEntity<List<Map<String, Object>>> getUserProjects(@PathVariable UUID userId) {
        log.debug("🔍 Internal: Fetching projects for user: {}", userId);

        List<ProjectMemberDTO> projects = projectMemberService.getUserProjects(userId);
        
        List<Map<String, Object>> projectsInfo = projects.stream()
            .map(pm -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", pm.getId());
                info.put("projectId", pm.getProjectId());
                info.put("tenantId", pm.getTenantId());
                info.put("role", pm.getRole().name());
                info.put("joinedAt", pm.getJoinedAt());
                return info;
            })
            .collect(Collectors.toList());

        log.debug("✅ User {} has access to {} projects", userId, projectsInfo.size());

        return ResponseEntity.ok(projectsInfo);
    }

    @GetMapping("/permissions/project/{projectId}/user/{userId}/role")
    public ResponseEntity<String> getProjectRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        log.debug("Internal: Getting project role for user {} in project {}", userId, projectId);

        String role = permissionService.getProjectRole(userId, projectId);

        if (role == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(role);
    }
}