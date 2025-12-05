package com.sentinel.user_management_service.client.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private UUID id;
    private UUID tenantId;
    private String name;
    private UUID ownerId;
}