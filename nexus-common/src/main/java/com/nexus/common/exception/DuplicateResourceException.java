package com.nexus.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends NexusException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

     public DuplicateResourceException(String message, HttpStatus status) {
        super(message, status);
    }
}
