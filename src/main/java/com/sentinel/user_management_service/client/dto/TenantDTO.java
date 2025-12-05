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
}