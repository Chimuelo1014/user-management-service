package com.sentinel.user_management_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
}