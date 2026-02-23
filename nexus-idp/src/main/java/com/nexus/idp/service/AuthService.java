package com.nexus.idp.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.nexus.common.exception.BadCredentialsException;
import com.nexus.common.exception.DuplicateResourceException;
import com.nexus.common.exception.ResourceNotFoundException;
import com.nexus.idp.config.RsaKeyProperties;
import com.nexus.idp.dto.LoginRequest;
import com.nexus.idp.dto.LoginResponse;
import com.nexus.idp.dto.RegisterRequest;
import com.nexus.idp.entity.Role;
import com.nexus.idp.entity.User;
import com.nexus.idp.repository.RoleRepository;
import com.nexus.idp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RsaKeyProperties rsaKeys;
    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${nexus.idp.token.expiration:3600}")
    private long tokenExpirationSeconds;


    public String getPublicKeyAsPem() {
        byte[] encoded = rsaKeys.publicKey().getEncoded();
        String base64encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN PUBLIC KEY-----\n" + base64encoded + "\n-----END PUBLIC KEY-----";
    }

    public LoginResponse login(LoginRequest request) {
        User user = verifyCredentials(request.getEmail(), request.getPassword());
        String jwt = generateUserToken(user);
        String sessionKey = generateSessionKeyAndSaveToRedis(user);
        return LoginResponse.builder()
                .accessToken(jwt)
                .sessionKey(sessionKey)
                .build();
    }

    private User verifyCredentials(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.isLocked()) {
            throw new BadCredentialsException("User account is locked");
        }

        return user;
    }

    private String generateUserToken(User user) {
        Instant now = Instant.now();

        String roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nexus-idp")
                .issuedAt(now)
                .expiresAt(now.plus(tokenExpirationSeconds, ChronoUnit.SECONDS))
                .subject(user.getEmail())
                .claim("roles", roles)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String generateSessionKeyAndSaveToRedis(User user) {
        String sessionKey = generateSessionKey();
        // Store session key in Redis with a TTL of 1 hour
        String redisKey = "session:" + user.getEmail().toLowerCase();
        redisTemplate.opsForValue().set(redisKey, sessionKey, Duration.of(1, ChronoUnit.HOURS));
        return sessionKey;
    }

    private String generateSessionKey() {
        // Generate a 256-bit AES Key (32 bytes)
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        Role defauRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        User newUser = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        newUser.getRoles().add(defauRole);

        userRepository.save(newUser);
    }
}
