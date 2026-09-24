package com.bank.ledger.gateway.filter;

import com.bank.ledger.dto.ProblemDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

@Component
public class JwtAuthGlobalFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

    private final ReactiveStringRedisTemplate reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Key key;

    // Endpoints that bypass authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/actuator",
            "/favicon.ico"
    );

    public JwtAuthGlobalFilter(ReactiveStringRedisTemplate reactiveRedisTemplate,
                               ObjectMapper objectMapper,
                               @Value("${app.security.jwt-secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Allow public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. Extract Bearer token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED,
                    "https://api.paypink.ph/errors/unauthorized",
                    "Authentication Required",
                    "A valid JWT Bearer token is required to access this endpoint.",
                    path);
        }

        String token = authHeader.substring(7);

        // 3. Validate signature & extract Claims
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception ex) {
            log.warn("JWT validation failed for path {}: {}", path, ex.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED,
                    "https://api.paypink.ph/errors/invalid-token",
                    "Invalid JWT Token",
                    "The provided JWT signature is invalid, expired, or malformed.",
                    path);
        }

        String jti = claims.getId();
        if (jti == null) {
            return onError(exchange, HttpStatus.UNAUTHORIZED,
                    "https://api.paypink.ph/errors/invalid-token-matrix",
                    "Malformed Token Claims",
                    "Token is missing mandatory 'jti' identifier.",
                    path);
        }

        // 4. Validate active state in Redis Token Matrix
        return reactiveRedisTemplate.hasKey("token:" + jti)
                .flatMap(hasKey -> {
                    if (Boolean.TRUE.equals(hasKey)) {
                        // Forward user details in headers to downstream microservices
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header("X-Auth-Username", claims.getSubject())
                                .header("X-Auth-Jti", jti)
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } else {
                        log.warn("Revoked token attempted for user {} (jti: {})", claims.getSubject(), jti);
                        return onError(exchange, HttpStatus.UNAUTHORIZED,
                                "https://api.paypink.ph/errors/revoked-token",
                                "Session Revoked",
                                "The session token has been revoked or expired from the active token matrix.",
                                path);
                    }
                })
                .onErrorResume(e -> {
                    // In case Redis is temporarily unreachable during gateway pre-check, proceed with signed token
                    log.error("Redis token check failed: {}, proceeding with cryptographic signature verification", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String type, String title, String detail, String instance) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.valueOf("application/problem+json"));

        ProblemDetails problem = new ProblemDetails(type, title, status.value(), detail, instance);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + ",\"title\":\"" + title + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the gateway filter chain
    }
}
