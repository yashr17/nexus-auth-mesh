package com.nexus.common.exception;

import org.springframework.http.HttpStatus;

public class TransactionException extends NexusException {
    public TransactionException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public TransactionException(String message, HttpStatus status) {
        super(message, status);
    }
}
