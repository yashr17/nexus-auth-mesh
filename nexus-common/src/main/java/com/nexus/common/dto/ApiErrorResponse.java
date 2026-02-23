package com.nexus.common.dto;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
