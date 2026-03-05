package com.nexus.common.exception;

import org.springframework.http.HttpStatus;

public class PayloadShieldException extends NexusException {
    public PayloadShieldException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public PayloadShieldException(String message, HttpStatus status) {
        super(message, status);
    }

    public PayloadShieldException(String message, Throwable cause) {
        super(message, cause);
    }

    public PayloadShieldException(String message, Throwable cause, HttpStatus status) {
        super(message, cause, status);
    }
}
