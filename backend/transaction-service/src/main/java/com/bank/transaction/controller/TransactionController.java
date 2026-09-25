package com.bank.transaction.controller;

import com.bank.common.dto.MutationRequest;
import com.bank.common.dto.MutationResponse;
import com.bank.transaction.service.LedgerMutationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final LedgerMutationService ledgerMutationService;

    public TransactionController(LedgerMutationService ledgerMutationService) {
        this.ledgerMutationService = ledgerMutationService;
    }

    @PostMapping("/mutate")
    public ResponseEntity<MutationResponse> mutateBalance(
            @Valid @RequestBody MutationRequest request,
            @RequestHeader(value = "X-User-Name", required = false, defaultValue = "demo_user") String username) {
        return ResponseEntity.ok(ledgerMutationService.mutateBalance(request, username));
    }
}
