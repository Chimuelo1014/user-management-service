package com.sentinel.user_management_service.service.impl;

import com.sentinel.user_management_service.dto.response.UserPlanDTO;
import com.sentinel.user_management_service.entity.UserPlanEntity;
import com.sentinel.user_management_service.enums.UserPlan;
import com.sentinel.user_management_service.exception.UserPlanNotFoundException;
import com.sentinel.user_management_service.repository.UserPlanRepository;
import com.sentinel.user_management_service.repository.TenantMemberRepository;
import com.sentinel.user_management_service.service.UserPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPlanServiceImpl implements UserPlanService {

    private final UserPlanRepository userPlanRepository;
    private final TenantMemberRepository tenantMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPlanDTO getUserPlan(UUID userId) {
        log.debug("Fetching plan for user: {}", userId);
        
        UserPlanEntity plan = userPlanRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPlanNotFoundException("Plan not found for user: " + userId));
        
        return mapToDTO(plan);
    }

    @Override
    @Transactional
    public UserPlanDTO createDefaultPlan(UUID userId) {
        log.info("Creating default FREE plan for user: {}", userId);
        
        // Verificar que no exista ya
        if (userPlanRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("User already has a plan");
        }

        UserPlanEntity plan = UserPlanEntity.builder()
                .userId(userId)
                .plan(UserPlan.FREE)
                .build();

        userPlanRepository.save(plan);
        log.info("✅ Default plan created for user: {}", userId);

        return mapToDTO(plan);
    }

    @Override
    @Transactional
    public UserPlanDTO upgradePlan(UUID userId, UserPlan newPlan) {
        log.info("Upgrading plan for user: {} to {}", userId, newPlan);
        
        UserPlanEntity plan = userPlanRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPlanNotFoundException("Plan not found for user: " + userId));

        plan.upgradePlan(newPlan);
        userPlanRepository.save(plan);

        log.info("✅ Plan upgraded for user: {} to {}", userId, newPlan);
        return mapToDTO(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateTenant(UUID userId) {
        UserPlanEntity plan = userPlanRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPlanNotFoundException("Plan not found"));

        long currentTenants = tenantMemberRepository.countDistinctTenantsByUserId(userId);
        boolean canCreate = plan.canCreateTenant((int) currentTenants);

        log.debug("User {} can create tenant: {} ({}/{})", userId, canCreate, currentTenants, plan.getMaxTenants());
        return canCreate;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateProject(UUID userId, int currentProjectCount) {
        UserPlanEntity plan = userPlanRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPlanNotFoundException("Plan not found"));

        return plan.canCreateProject(currentProjectCount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddUserToTenant(UUID userId, int currentUserCount) {
        UserPlanEntity plan = userPlanRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPlanNotFoundException("Plan not found"));

        return plan.canAddUserToTenant(currentUserCount);
    }

    private UserPlanDTO mapToDTO(UserPlanEntity entity) {
        return UserPlanDTO.builder()
                .userId(entity.getUserId())
                .plan(entity.getPlan().name())
                .maxTenants(entity.getMaxTenants())
                .maxProjectsPerTenant(entity.getMaxProjectsPerTenant())
                .maxUsersPerTenant(entity.getMaxUsersPerTenant())
                .maxScansPerMonth(entity.getMaxScansPerMonth())
                .assignedAt(entity.getAssignedAt())
                .build();
    }
}