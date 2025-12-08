package com.sentinel.user_management_service.client.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDTO {
    private UUID id;
    private String name;
    private String plan;
    private TenantUsageDTO usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantUsageDTO {
        private int currentUsers;
        private int currentProjects;
        private int currentDomains;
        private int currentRepos;
    }
}