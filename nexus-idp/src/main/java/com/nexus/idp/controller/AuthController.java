package com.nexus.idp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.idp.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/public-key")
    public String getPublicKey() {
        return authService.getPublicKeyAsPem();
    }

    @GetMapping("/test-token")
    public String generateToken() {
        return authService.generateTestToken("nexus_admin");
    }
}
