package com.sentinel.user_management_service.dto.response;

import com.sentinel.user_management_service.enums.TenantRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantWithRoleDTO {
    
    private UUID tenantId;
    private String tenantName;
    private String plan;
    private TenantRole role;
    private LocalDateTime joinedAt;
}
