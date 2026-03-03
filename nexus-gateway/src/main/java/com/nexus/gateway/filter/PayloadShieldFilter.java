package com.nexus.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.nexus.common.exception.PayloadShieldException;
import com.nexus.gateway.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PayloadShieldFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(PayloadShieldFilter.class);
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public int getOrder() {
        return -10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/auth/")) {
            log.info("PayloadShield bypassed for public auth route {}", path);
            return chain.filter(exchange);
        }

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId == null) {
            log.warn("PayloadShield blocked request: Missing X-User-Id for protected path {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String redisKey = "session:" + userId;
        return redisTemplate.opsForValue().get(redisKey)
                .switchIfEmpty(Mono.error(new RuntimeException("AES Session Key missing in Redis for user: " + userId)))
                .flatMap(aesKey -> {
                    log.info("PayloadShield: Successfully retrieved AES Session Key for user {}", userId);

                    ServerHttpRequest decoratedRequest = decryptRequest(exchange.getRequest(), aesKey);
                    ServerHttpResponse decoratedResponse = encryptResponse(exchange.getResponse(), aesKey);

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(decoratedRequest)
                            .response(decoratedResponse)
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(PayloadShieldException.class, e -> {
                    log.error("PayloadShield Security Block: {}", e.getMessage(), e);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    private ServerHttpRequest decryptRequest(ServerHttpRequest request, String aesKey) {
        return new ServerHttpRequestDecorator(request) {

            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody().collectList().flatMapMany(dataBuffers -> {
                    DataBufferFactory factory = new DefaultDataBufferFactory();

                    DataBuffer joinedBuffer = factory.join(dataBuffers);
                    byte[] incomingBytes = new byte[joinedBuffer.readableByteCount()];
                    joinedBuffer.read(incomingBytes);

                    DataBufferUtils.release(joinedBuffer);

                    try {
                        byte[] encryptedBytes = Base64.getDecoder().decode(incomingBytes);
                        String plainJson = AesCryptoUtil.decrypt(encryptedBytes, aesKey);
                        log.info("Decrypted Payload: {}", plainJson);

                        byte[] plainBytes = plainJson.getBytes(StandardCharsets.UTF_8);
                        DataBuffer newBuffer = factory.wrap(plainBytes);

                        return Flux.just(newBuffer);
                    } catch (Exception e) {
                        return Flux.error(new PayloadShieldException("Failed to decrypt inbound payload", e));
                    }
                });
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.remove(HttpHeaders.CONTENT_LENGTH);

                String targetContentType = headers.getFirst("X-Target-Content-Type");
                if(targetContentType == null) {
                    targetContentType = MediaType.APPLICATION_JSON_VALUE; // Default to JSON if not specified
                }
                headers.set(HttpHeaders.CONTENT_TYPE, targetContentType); 
                headers.remove("X-Target-Content-Type");
                return headers;
            }
        };
    }

    private ServerHttpResponse encryptResponse(ServerHttpResponse response, String aesKey) {
        return new ServerHttpResponseDecorator(response) {
            @SuppressWarnings("null")
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

                Flux<DataBuffer> fluxBody = Flux.from(body);

                return super.writeWith(fluxBody.collectList().flatMap(dataBuffers -> {

                    if (dataBuffers.isEmpty()) {
                        return Mono.empty();
                    }

                    DataBufferFactory factory = new DefaultDataBufferFactory();

                    DataBuffer joinedBuffer = factory.join(dataBuffers);
                    byte[] plainBytes = new byte[joinedBuffer.readableByteCount()];
                    joinedBuffer.read(plainBytes);

                    DataBufferUtils.release(joinedBuffer);

                    try {
                        byte[] encryptedBlob = AesCryptoUtil.encrypt(plainBytes, aesKey);
                        byte[] base64blob = Base64.getEncoder().encode(encryptedBlob);
                        log.debug("Successfully encrypted outbound payload ({} bytes)", base64blob.length);
                        DataBuffer newBuffer = factory.wrap(base64blob);

                        String originalContentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                        getHeaders().set("X-Original-Content-Type", originalContentType != null ? originalContentType : MediaType.APPLICATION_JSON_VALUE);

                        getHeaders().setContentLength(base64blob.length);
                        getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);

                        return Mono.just(newBuffer);

                    } catch (Exception e) {
                        log.error("Failed to encrypt outbound payload", e);
                        return Mono.error(new PayloadShieldException("Encryption failed", e));
                    }
                }));
            }
        };
    }
}
