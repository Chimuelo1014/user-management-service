package com.sentinel.user_management_service.service;

import java.util.UUID;

public interface PermissionService {
    
    boolean checkPermission(UUID userId, UUID tenantId, UUID projectId, String permission);
    
    boolean canManageTenant(UUID userId, UUID tenantId);
    
    boolean canManageProject(UUID userId, UUID projectId);
    
    boolean canCreateScans(UUID userId, UUID projectId);
    
    boolean canViewScans(UUID userId, UUID projectId);
    
    String getTenantRole(UUID tenantId, UUID userId);
    
    String getProjectRole(UUID projectId, UUID userId);
}