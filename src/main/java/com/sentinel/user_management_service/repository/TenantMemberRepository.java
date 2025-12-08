package com.sentinel.user_management_service.repository;

import com.sentinel.user_management_service.entity.TenantMemberEntity;
import com.sentinel.user_management_service.enums.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantMemberRepository extends JpaRepository<TenantMemberEntity, UUID> {

    Optional<TenantMemberEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    List<TenantMemberEntity> findByTenantId(UUID tenantId);

    List<TenantMemberEntity> findByUserId(UUID userId);

    List<TenantMemberEntity> findByTenantIdAndRole(UUID tenantId, TenantRole role);

    boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Query("SELECT COUNT(m) FROM TenantMemberEntity m WHERE m.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM TenantMemberEntity m WHERE m.tenantId = :tenantId AND m.role = :role")
    long countByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") TenantRole role);

    @Query("SELECT COUNT(DISTINCT m.tenantId) FROM TenantMemberEntity m WHERE m.userId = :userId")
    long countDistinctTenantsByUserId(@Param("userId") UUID userId);

    void deleteByTenantIdAndUserId(UUID tenantId, UUID userId);
}

