package com.nexus.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends NexusException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, HttpStatus status) {
        super(message, status);
    }
}
