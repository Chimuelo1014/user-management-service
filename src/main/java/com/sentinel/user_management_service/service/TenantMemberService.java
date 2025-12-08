package com.sentinel.user_management_service.service;

import com.sentinel.user_management_service.dto.response.TenantMemberDTO;
import com.sentinel.user_management_service.dto.response.TenantWithRoleDTO;
import com.sentinel.user_management_service.client.dto.TenantDTO;
import com.sentinel.user_management_service.enums.TenantRole;

import java.util.List;
import java.util.UUID;

public interface TenantMemberService {
    
    TenantMemberDTO addMember(UUID tenantId, UUID userId, TenantRole role, UUID invitedBy);
    
    List<TenantMemberDTO> getTenantMembers(UUID tenantId);
    
    List<TenantMemberDTO> getUserTenants(UUID userId);
    
    List<TenantWithRoleDTO> getUserTenantsWithRole(UUID userId);
    
    TenantMemberDTO getMemberRole(UUID tenantId, UUID userId);
    
    void removeMember(UUID tenantId, UUID userId, UUID requestingUserId);
    
    TenantMemberDTO updateMemberRole(UUID tenantId, UUID userId, TenantRole newRole, UUID requestingUserId);
    
    boolean isMember(UUID tenantId, UUID userId);
    
    boolean isAdmin(UUID tenantId, UUID userId);
    
    long countMembers(UUID tenantId);

     List<UUID> getUserTenantIds(UUID userId);
}
