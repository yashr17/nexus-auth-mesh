package com.nexus.common.exception;

import org.springframework.http.HttpStatus;

public class BadCredentialsException extends NexusException {
    public BadCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

     public BadCredentialsException(String message, HttpStatus status) {
        super(message, status);
    }
}
