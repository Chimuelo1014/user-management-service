package com.sentinel.user_management_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlanDTO {
    
    private UUID userId;
    private String plan;
    private int maxTenants;
    private int maxProjectsPerTenant;
    private int maxUsersPerTenant;
    private int maxScansPerMonth;
    private LocalDateTime assignedAt;
}