package com.nexus.gateway.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class JwtValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);
    private final RSAPublicKey publicKey;

    public JwtValidator(@Value("${nexus.security.jwt.public-key}") Resource publicKeyResource) throws Exception {
        this.publicKey = loadPublicKey(publicKeyResource);
        log.info("RSA Public key loaded successfully from {}", publicKeyResource.getFilename());
    }

    private RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        String keyData;
        try (var inputStream = resource.getInputStream()) {
            keyData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        String publicKeyPEM = keyData
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(this.publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
