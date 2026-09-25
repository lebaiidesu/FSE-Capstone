package com.bank.reconciliation.controller;

import com.bank.reconciliation.model.ReconciliationLog;
import com.bank.reconciliation.service.ReconciliationSweepService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationSweepService reconciliationService;

    public ReconciliationController(ReconciliationSweepService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<ReconciliationLog>> getLogs() {
        return ResponseEntity.ok(reconciliationService.getAllLogs());
    }
}
