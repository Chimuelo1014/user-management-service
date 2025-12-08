package com.sentinel.user_management_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitValidationResponse {
    private boolean allowed;
    private int maxLimit;
    private int currentUsage;
    private String message;
    private String upgradePlanHint;
}
