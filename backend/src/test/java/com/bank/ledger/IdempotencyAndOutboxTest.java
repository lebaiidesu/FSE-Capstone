package com.bank.ledger;

import com.bank.ledger.dto.MutationRequest;
import com.bank.ledger.dto.MutationResponse;
import com.bank.ledger.model.oracle.Account;
import com.bank.ledger.model.oracle.OutboxEvent;
import com.bank.ledger.model.postgres.LedgerMutationAudit;
import com.bank.ledger.model.postgres.ReconciliationLog;
import com.bank.ledger.repository.oracle.AccountRepository;
import com.bank.ledger.repository.oracle.OutboxEventRepository;
import com.bank.ledger.repository.postgres.LedgerMutationAuditRepository;
import com.bank.ledger.repository.postgres.ReconciliationLogRepository;
import com.bank.ledger.service.AccountService;
import com.bank.ledger.service.LedgerMutationService;
import com.bank.ledger.service.ReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class IdempotencyAndOutboxTest {

    @Autowired
    private LedgerMutationService ledgerMutationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private LedgerMutationAuditRepository ledgerMutationAuditRepository;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconciliationLogRepository reconciliationLogRepository;

    @Test
    @DisplayName("Requirement 1.D & 2: Redis Idempotency Check (<5ms turnaround & zero duplicate mutation)")
    public void testIdempotencyDeduplication() {
        Account account = accountRepository.findAll().get(0);
        accountService.resetAccountBalance(account.getAccountId(), new BigDecimal("1000.0000"));

        String idempotencyKey = "IDEMP-" + UUID.randomUUID();
        MutationRequest req = new MutationRequest(account.getAccountId(), new BigDecimal("100.0000"), "DEBIT", "DEBIT", null, "PHP");
        req.setIdempotencyKey(idempotencyKey);

        // 1. Initial Request
        MutationResponse firstResponse = ledgerMutationService.mutateBalance(req, "test_user");
        assertFalse(firstResponse.isCachedIdempotentResponse());
        assertEquals(new BigDecimal("900.0000"), firstResponse.getAfterBalance());

        // 2. Duplicate Request with identical key
        long tStart = System.nanoTime();
        MutationResponse duplicateResponse = ledgerMutationService.mutateBalance(req, "test_user");
        long latencyMs = (System.nanoTime() - tStart) / 1_000_000;

        assertTrue(duplicateResponse.isCachedIdempotentResponse(), "Second request must be served from cache");
        assertEquals(firstResponse.getTransactionId(), duplicateResponse.getTransactionId());
        assertTrue(latencyMs <= 5, "In-memory idempotency check must complete in <= 5ms SLA");

        // Verify balance was only deducted once
        Account finalAccount = accountRepository.findById(account.getAccountId()).orElseThrow();
        assertEquals(new BigDecimal("900.0000"), finalAccount.getCurrentBalance());
    }

    @Test
    @DisplayName("Event Lifecycle: Outbox Event + PostgreSQL Audit Append + @Scheduled Reconciliation")
    public void testEventLifecycleAndReconciliation() {
        Account account = accountRepository.findAll().get(0);
        accountService.resetAccountBalance(account.getAccountId(), new BigDecimal("5000.0000"));

        MutationRequest req = new MutationRequest(account.getAccountId(), new BigDecimal("500.0000"), "CREDIT", "CREDIT", null, "PHP");
        MutationResponse res = ledgerMutationService.mutateBalance(req, "test_user");

        // Allow async thread to process or trigger poll
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {}

        // Verify Outbox Event created
        List<OutboxEvent> outboxList = outboxEventRepository.findAll();
        assertFalse(outboxList.isEmpty(), "Outbox table must contain event");

        // Verify PostgreSQL Immutable Audit Log entry
        Optional<LedgerMutationAudit> auditOpt = ledgerMutationAuditRepository.findByTransactionId(res.getTransactionId());
        assertTrue(auditOpt.isPresent(), "PostgreSQL LEDGER_MUTATION_AUDIT must contain record for transaction");
        assertEquals(0, new BigDecimal("500.0000").compareTo(auditOpt.get().getAmount()));

        // Run scheduled reconciliation sweep
        reconciliationService.runScheduledSystemWideReconciliation();
        Optional<ReconciliationLog> reconLog = reconciliationLogRepository.findTopByTransactionIdOrderByReconDateDesc(res.getTransactionId());
        assertTrue(reconLog.isPresent(), "Reconciliation log must exist");
        assertEquals("MATCHED", reconLog.get().getReconStatus());
    }
}
