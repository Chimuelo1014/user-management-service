package com.sentinel.user_management_service.service;

import com.sentinel.user_management_service.dto.response.UserPlanDTO;
import com.sentinel.user_management_service.enums.UserPlan;

import java.util.UUID;

public interface UserPlanService {
    
    UserPlanDTO getUserPlan(UUID userId);
    
    UserPlanDTO createDefaultPlan(UUID userId);
    
    UserPlanDTO upgradePlan(UUID userId, UserPlan newPlan);
    
    boolean canCreateTenant(UUID userId);
    
    boolean canCreateProject(UUID userId, int currentProjectCount);
    
    boolean canAddUserToTenant(UUID userId, int currentUserCount);
}