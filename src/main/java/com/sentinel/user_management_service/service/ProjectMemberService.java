package com.sentinel.user_management_service.service;

import com.sentinel.user_management_service.dto.response.ProjectMemberDTO;
import com.sentinel.user_management_service.dto.response.ProjectWithDetailsDTO;
import com.sentinel.user_management_service.client.dto.ProjectDTO;
import com.sentinel.user_management_service.enums.ProjectRole;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberService {
    
    ProjectMemberDTO addMember(UUID projectId, UUID userId, UUID tenantId, ProjectRole role, UUID addedBy);
    
    List<ProjectMemberDTO> getProjectMembers(UUID projectId);
    
    List<ProjectMemberDTO> getUserProjects(UUID userId);
    
    List<ProjectWithDetailsDTO> getUserProjectsWithDetails(UUID userId);
    
    ProjectMemberDTO getMemberRole(UUID projectId, UUID userId);
    
    void removeMember(UUID projectId, UUID userId, UUID requestingUserId);
    
    ProjectMemberDTO updateMemberRole(UUID projectId, UUID userId, ProjectRole newRole, UUID requestingUserId);
    
    boolean isMember(UUID projectId, UUID userId);
    
    boolean isAdmin(UUID projectId, UUID userId);
    
    long countMembers(UUID projectId);
}
