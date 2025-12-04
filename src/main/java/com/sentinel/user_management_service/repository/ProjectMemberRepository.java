package com.sentinel.user_management_service.repository;

import com.sentinel.user_management_service.entity.ProjectMemberEntity;
import com.sentinel.user_management_service.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, UUID> {

    Optional<ProjectMemberEntity> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMemberEntity> findByProjectId(UUID projectId);

    List<ProjectMemberEntity> findByUserId(UUID userId);

    List<ProjectMemberEntity> findByTenantId(UUID tenantId);

    List<ProjectMemberEntity> findByProjectIdAndRole(UUID projectId, ProjectRole role);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("SELECT COUNT(m) FROM ProjectMemberEntity m WHERE m.projectId = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT m FROM ProjectMemberEntity m WHERE m.userId = :userId AND m.tenantId = :tenantId")
    List<ProjectMemberEntity> findByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);
}
