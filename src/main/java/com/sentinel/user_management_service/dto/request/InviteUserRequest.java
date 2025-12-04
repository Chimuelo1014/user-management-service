package com.sentinel.user_management_service.dto.request;

import com.sentinel.user_management_service.enums.InvitationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteUserRequest {
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
    
    // These will be auto-filled by the controller
    private InvitationType type;
    
    private UUID resourceId;
    
    private String resourceName;
    
    @NotBlank(message = "Role is required")
    private String role;
}