package com.nexus.gateway.filter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.nexus.gateway.security.JwtValidator;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtValidator jwtValidator;

    @Value("${nexus.security.open-endpoints}")
    private List<String> openEndpoints;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if(openEndpoints.stream().anyMatch(path::contains)) {
            log.debug("Permitting public endpoint: {}", path);
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.warn("Missing Authorization header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        try{
            Claims claims = jwtValidator.validateToken(token);
            String userId = claims.getSubject();

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            log.debug("JWT valid. Forwarding request {} with X-User-Id {}", path, userId);
            return chain.filter(mutatedExchange);
        } catch (Exception e) {
            log.error("Error validating JWT token for path: {} | Exception: {}", path, e);
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
