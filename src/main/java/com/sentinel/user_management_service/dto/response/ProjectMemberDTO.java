package com.sentinel.user_management_service.dto.response;

import com.sentinel.user_management_service.enums.ProjectRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDTO {
    
    private UUID id;
    private UUID projectId;
    private UUID userId;
    private UUID tenantId;
    private ProjectRole role;
    private LocalDateTime joinedAt;
    private UUID addedBy;
    private LocalDateTime createdAt;
}