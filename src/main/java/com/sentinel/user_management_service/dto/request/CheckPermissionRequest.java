package com.sentinel.user_management_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPermissionRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private UUID tenantId;
    
    private UUID projectId;
    
    @NotBlank(message = "Permission is required")
    private String permission;
}