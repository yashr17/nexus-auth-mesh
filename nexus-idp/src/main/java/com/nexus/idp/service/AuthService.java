package com.nexus.idp.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.nexus.idp.config.RsaKeyProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RsaKeyProperties rsaKeys;
    private final JwtEncoder jwtEncoder;

    public String getPublicKeyAsPem() {
        byte[] encoded = rsaKeys.publicKey().getEncoded();
        String base64encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN PUBLIC KEY-----\n" + base64encoded + "\n-----END PUBLIC KEY-----";
    }

    public String generateTestToken(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nexus-idp")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(subject)
                .claim("roles", "ROLE_USER")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
