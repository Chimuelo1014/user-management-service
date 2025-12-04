package com.sentinel.user_management_service.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckResponse {
    
    private boolean allowed;
    private UUID userId;
    private UUID tenantId;
    private UUID projectId;
    private String permission;
    private String reason;
}