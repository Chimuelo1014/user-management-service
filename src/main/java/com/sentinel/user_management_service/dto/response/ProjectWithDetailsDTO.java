package com.sentinel.user_management_service.dto.response;

import com.sentinel.user_management_service.enums.ProjectRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectWithDetailsDTO {
    
    private UUID projectId;
    private UUID tenantId;
    private String projectName;
    private ProjectRole role;
    private LocalDateTime joinedAt;
}
