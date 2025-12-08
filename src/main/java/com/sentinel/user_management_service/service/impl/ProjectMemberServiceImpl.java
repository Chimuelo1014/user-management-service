package com.sentinel.user_management_service.service.impl;

import com.sentinel.user_management_service.dto.response.ProjectMemberDTO;
import com.sentinel.user_management_service.dto.response.ProjectWithDetailsDTO;
import com.sentinel.user_management_service.entity.ProjectMemberEntity;
import com.sentinel.user_management_service.enums.ProjectRole;
import com.sentinel.user_management_service.exception.MemberAlreadyExistsException;
import com.sentinel.user_management_service.exception.MemberNotFoundException;
import com.sentinel.user_management_service.exception.PermissionDeniedException;
import com.sentinel.user_management_service.repository.ProjectMemberRepository;
import com.sentinel.user_management_service.service.ProjectMemberService;
import com.sentinel.user_management_service.client.ProjectServiceClient;
import com.sentinel.user_management_service.client.dto.ProjectDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectServiceClient projectServiceClient;

    @Override
    @Transactional
    public ProjectMemberDTO addMember(UUID projectId, UUID userId, UUID tenantId, ProjectRole role, UUID addedBy) {
        log.info("Adding member {} to project {} with role {}", userId, projectId, role);

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new MemberAlreadyExistsException("User is already a member of this project");
        }

        ProjectMemberEntity member = ProjectMemberEntity.builder()
                .projectId(projectId)
                .userId(userId)
                .tenantId(tenantId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .addedBy(addedBy)
                .build();

        projectMemberRepository.save(member);

        log.info("Project member added successfully: {}", member.getId());

        return mapToDTO(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberDTO> getProjectMembers(UUID projectId) {
        return projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberDTO> getUserProjects(UUID userId) {
        return projectMemberRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWithDetailsDTO> getUserProjectsWithDetails(UUID userId) {
        log.debug("Fetching projects with details for user: {}", userId);
        
        List<ProjectMemberEntity> memberEntities = projectMemberRepository.findByUserId(userId);
        
        List<ProjectWithDetailsDTO> projects = memberEntities.stream()
                .map(member -> {
                    try {
                        ProjectDTO projectDTO = projectServiceClient.getProject(member.getProjectId());
                        return ProjectWithDetailsDTO.builder()
                                .projectId(projectDTO.getId())
                                .tenantId(projectDTO.getTenantId())
                                .projectName(projectDTO.getName())
                                .role(member.getRole())
                                .joinedAt(member.getJoinedAt())
                                .build();
                    } catch (Exception e) {
                        log.warn("Could not fetch project details for {}: {}", member.getProjectId(), e.getMessage());
                        return ProjectWithDetailsDTO.builder()
                                .projectId(member.getProjectId())
                                .tenantId(member.getTenantId())
                                .projectName("Unknown Project")
                                .role(member.getRole())
                                .joinedAt(member.getJoinedAt())
                                .build();
                    }
                })
                .collect(Collectors.toList());
        
        log.debug("Fetched {} projects with details for user {}", projects.size(), userId);
        return projects;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMemberDTO getMemberRole(UUID projectId, UUID userId) {
        ProjectMemberEntity member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new MemberNotFoundException("User is not a member of this project"));

        return mapToDTO(member);
    }

    @Override
    @Transactional
    public void removeMember(UUID projectId, UUID userId, UUID requestingUserId) {
        log.info("Removing member {} from project {} by user {}", userId, projectId, requestingUserId);

        // Verify requesting user is admin
        ProjectMemberEntity requestingMember = projectMemberRepository.findByProjectIdAndUserId(projectId, requestingUserId)
                .orElseThrow(() -> new PermissionDeniedException("You are not a member of this project"));

        if (!requestingMember.isAdmin()) {
            throw new PermissionDeniedException("Only admins can remove members");
        }

        // Verify target member exists
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new MemberNotFoundException("User is not a member of this project");
        }

        // Count admins
        long adminCount = projectMemberRepository.findByProjectIdAndRole(projectId, ProjectRole.PROJECT_ADMIN).size();

        // Don't allow removing last admin
        ProjectMemberEntity targetMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId).get();
        if (targetMember.isAdmin() && adminCount <= 1) {
            throw new PermissionDeniedException("Cannot remove last admin from project");
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);

        log.info("Project member removed successfully");
    }

    @Override
    @Transactional
    public ProjectMemberDTO updateMemberRole(UUID projectId, UUID userId, ProjectRole newRole, UUID requestingUserId) {
        log.info("Updating member {} role to {} in project {}", userId, newRole, projectId);

        // Verify requesting user is admin
        ProjectMemberEntity requestingMember = projectMemberRepository.findByProjectIdAndUserId(projectId, requestingUserId)
                .orElseThrow(() -> new PermissionDeniedException("You are not a member of this project"));

        if (!requestingMember.isAdmin()) {
            throw new PermissionDeniedException("Only admins can update member roles");
        }

        // Get target member
        ProjectMemberEntity targetMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new MemberNotFoundException("User is not a member of this project"));

        // Don't allow removing admin from last admin
        if (targetMember.isAdmin() && newRole != ProjectRole.PROJECT_ADMIN) {
            long adminCount = projectMemberRepository.findByProjectIdAndRole(projectId, ProjectRole.PROJECT_ADMIN).size();
            if (adminCount <= 1) {
                throw new PermissionDeniedException("Cannot demote last admin");
            }
        }

        targetMember.setRole(newRole);
        projectMemberRepository.save(targetMember);

        log.info("Project member role updated successfully");

        return mapToDTO(targetMember);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMemberEntity::isAdmin)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMembers(UUID projectId) {
        return projectMemberRepository.countByProjectId(projectId);
    }

    // Helper method
    private ProjectMemberDTO mapToDTO(ProjectMemberEntity entity) {
        return ProjectMemberDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .userId(entity.getUserId())
                .tenantId(entity.getTenantId())
                .role(entity.getRole())
                .joinedAt(entity.getJoinedAt())
                .addedBy(entity.getAddedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}