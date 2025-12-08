package com.sentinel.user_management_service.repository;

import com.sentinel.user_management_service.entity.UserPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPlanRepository extends JpaRepository<UserPlanEntity, UUID> {
    Optional<UserPlanEntity> findByUserId(UUID userId);
}