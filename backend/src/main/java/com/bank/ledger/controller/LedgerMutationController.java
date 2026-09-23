package com.bank.ledger.controller;

import com.bank.ledger.dto.MutationRequest;
import com.bank.ledger.dto.MutationResponse;
import com.bank.ledger.service.LedgerMutationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ledger")
@CrossOrigin(origins = "*")
public class LedgerMutationController {

    private final LedgerMutationService ledgerMutationService;

    public LedgerMutationController(LedgerMutationService ledgerMutationService) {
        this.ledgerMutationService = ledgerMutationService;
    }

    /**
     * Requirement 1.A: Strictly enforces JSR-380 validation (@Digits(14,4), @Positive).
     * Intercepted by GlobalExceptionHandler on violation.
     */
    @PostMapping("/mutate")
    public ResponseEntity<MutationResponse> mutateLedger(
            @Valid @RequestBody MutationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            Authentication authentication) {

        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            request.setIdempotencyKey(idempotencyKeyHeader);
        }

        String username = authentication != null ? authentication.getName() : "jdelacruz";
        MutationResponse response = ledgerMutationService.mutateBalance(request, username);
        return ResponseEntity.ok(response);
    }
}
