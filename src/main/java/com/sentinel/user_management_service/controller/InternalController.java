package com.sentinel.user_management_service.controller;

import com.sentinel.user_management_service.dto.request.CheckPermissionRequest;
import com.sentinel.user_management_service.dto.response.PermissionCheckResponse;
import com.sentinel.user_management_service.service.PermissionService;
import com.sentinel.user_management_service.service.TenantMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

        private final PermissionService permissionService;
        private final TenantMemberService tenantMemberService;

        @PostMapping("/permissions/check")
        public ResponseEntity<PermissionCheckResponse> checkPermission(
                        @Valid @RequestBody CheckPermissionRequest request) {
                boolean hasPermission = permissionService.checkPermission(
                                request.getUserId(),
                                request.getResourceType(),
                                request.getResourceId(),
                                request.getRequiredRole());
                return ResponseEntity.ok(new PermissionCheckResponse(hasPermission));
        }

        @GetMapping("/permissions/tenant/{tenantId}/user/{userId}/role")
        public ResponseEntity<String> getTenantRole(
                        @PathVariable UUID tenantId,
                        @PathVariable UUID userId) {
                log.debug("Internal: Getting tenant role for user {} in tenant {}", userId, tenantId);

                String role = tenantMemberService.getTenantRole(userId, tenantId);

                if (role == null) {
                        log.debug("User {} is not a member of tenant {}", userId, tenantId);
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(role);
        }

        /**
         * ✅ NUEVO ENDPOINT: Obtener lista de tenants donde el usuario es miembro
         * GET /api/internal/users/{userId}/tenants
         * 
         * Usado por tenant-service para el endpoint GET /api/tenants/me
         */
        @GetMapping("/users/{userId}/tenants")
        public ResponseEntity<List<UUID>> getUserTenants(@PathVariable UUID userId) {
                log.debug("🔍 Internal: Fetching tenants for user: {}", userId);

                List<UUID> tenantIds = tenantMemberService.getUserTenantIds(userId);

                log.debug("✅ User {} is member of {} tenants: {}",
                                userId, tenantIds.size(), tenantIds);

                return ResponseEntity.ok(tenantIds);
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