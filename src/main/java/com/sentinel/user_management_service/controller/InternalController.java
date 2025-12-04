package com.sentinel.user_management_service.controller;

import com.sentinel.user_management_service.dto.request.CheckPermissionRequest;
import com.sentinel.user_management_service.dto.response.PermissionCheckResponse;
import com.sentinel.user_management_service.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/internal/permissions")
@RequiredArgsConstructor
public class InternalController {

    private final PermissionService permissionService;

    @PostMapping("/check")
    public ResponseEntity<PermissionCheckResponse> checkPermission(
            @Valid @RequestBody CheckPermissionRequest request
    ) {
        boolean hasPermission = permissionService.checkPermission(
                request.getUserId(),
                request.getTenantId(),
                request.getProjectId(),
                request.getPermission()
        );

        return ResponseEntity.ok(PermissionCheckResponse.builder()
                .allowed(hasPermission)
                .userId(request.getUserId())
                .tenantId(request.getTenantId())
                .projectId(request.getProjectId())
                .permission(request.getPermission())
                .build());
    }

    @GetMapping("/tenant/{tenantId}/user/{userId}/role")
    public ResponseEntity<String> getTenantRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId
    ) {
        String role = permissionService.getTenantRole(tenantId, userId);
        return ResponseEntity.ok(role);
    }
}