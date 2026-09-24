package com.bank.ledger.security;

import com.bank.ledger.dto.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;

@Component
public class IdempotencyHandlerInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyHandlerInterceptor(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/ledger/mutate")) {
            return true;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            writeRfc7807Error(response, HttpStatus.BAD_REQUEST,
                    "https://api.paypink.ph/errors/missing-idempotency-key",
                    "Missing Idempotency-Key",
                    "A unique 'Idempotency-Key' HTTP header is mandatory for all balance mutation requests.",
                    path);
            return false;
        }

        String state = idempotencyService.get(idempotencyKey);
        if (state != null) {
            if (IdempotencyService.IN_PROGRESS.equals(state)) {
                writeRfc7807Error(response, HttpStatus.CONFLICT,
                        "https://api.paypink.ph/errors/concurrent-mutation-in-progress",
                        "Concurrent Mutation In Progress",
                        "A mutation request with Idempotency-Key '" + idempotencyKey + "' is currently being processed by the core engine.",
                        path);
                return false;
            } else {
                // Cached completed JSON response
                response.setStatus(HttpStatus.OK.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Idempotent-Replay", "true");
                response.getWriter().write(state);
                response.getWriter().flush();
                return false;
            }
        }

        // Key is absent, attempt atomic reservation
        boolean reserved = idempotencyService.reserve(idempotencyKey);
        if (!reserved) {
            writeRfc7807Error(response, HttpStatus.CONFLICT,
                    "https://api.paypink.ph/errors/concurrent-mutation-in-progress",
                    "Concurrent Mutation In Progress",
                    "A mutation request with Idempotency-Key '" + idempotencyKey + "' is currently being processed by the core engine.",
                    path);
            return false;
        }

        return true;
    }

    private void writeRfc7807Error(HttpServletResponse response, HttpStatus status, String type,
                                   String title, String detail, String instance) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        ProblemDetails problem = new ProblemDetails(
                type,
                title,
                status.value(),
                detail,
                instance
        );
        response.getWriter().write(objectMapper.writeValueAsString(problem));
        response.getWriter().flush();
    }

        @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Success: afterCommit() already stored the response; keep it
        if (ex == null && response.getStatus() < 400) {
            return;
        }
        String key = request.getHeader("Idempotency-Key");
        if (key != null && !key.isBlank()) {
            idempotencyService.releaseIfInProgress(key);
        }
    }
}
