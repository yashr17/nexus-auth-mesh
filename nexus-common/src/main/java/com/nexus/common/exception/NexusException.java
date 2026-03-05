package com.nexus.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class NexusException extends RuntimeException {

    private HttpStatus status;

    public NexusException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public NexusException(String message, Throwable cause) {
        super(message, cause);
    }

    public NexusException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
}
