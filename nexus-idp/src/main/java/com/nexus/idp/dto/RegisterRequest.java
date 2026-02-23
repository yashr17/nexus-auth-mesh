package com.nexus.idp.dto;

import com.nexus.common.validation.ValidPattern;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

                @NotBlank(message = "Email is required")
                @ValidPattern(name = "email", message = "Invalid email format")
                String email,

                @NotBlank(message = "Password is required")
                @Size(min = 8, message = "Password must be at least 8 characters long")
                @ValidPattern(name = "password", message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
                String password) {
}
