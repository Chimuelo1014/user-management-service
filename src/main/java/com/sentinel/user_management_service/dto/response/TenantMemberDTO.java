package com.sentinel.user_management_service.dto.response;

import com.sentinel.user_management_service.enums.TenantRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberDTO {
    
    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private TenantRole role;
    private LocalDateTime joinedAt;
    private UUID invitedBy;
    private LocalDateTime createdAt;
}