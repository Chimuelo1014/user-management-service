package com.sentinel.user_management_service.exception;

public class UserPlanNotFoundException extends RuntimeException {
    
    public UserPlanNotFoundException(String message) {
        super(message);
    }

    public UserPlanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
