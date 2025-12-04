package com.sentinel.user_management_service.exception;

public class InvitationExpiredException extends RuntimeException {
    public InvitationExpiredException(String message) {
        super(message);
    }
}
