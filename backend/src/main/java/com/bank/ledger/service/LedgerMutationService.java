package com.bank.ledger.service;

import com.bank.ledger.dto.MutationRequest;
import com.bank.ledger.dto.MutationResponse;
import com.bank.ledger.exception.AccountNotFoundException;
import com.bank.ledger.exception.CurrencyMismatchException;
import com.bank.ledger.exception.InsufficientFundsException;
import com.bank.ledger.model.oracle.Account;
import com.bank.ledger.model.oracle.AuditLog;
import com.bank.ledger.model.oracle.OutboxEvent;
import com.bank.ledger.model.oracle.TransactionRecord;
import com.bank.ledger.repository.oracle.AccountRepository;
import com.bank.ledger.repository.oracle.AuditLogRepository;
import com.bank.ledger.repository.oracle.OutboxEventRepository;
import com.bank.ledger.repository.oracle.TransactionRepository;
import com.bank.ledger.security.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LedgerMutationService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxPublisherService outboxPublisherService;
    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;

    public LedgerMutationService(AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 AuditLogRepository auditLogRepository,
                                 IdempotencyService idempotencyService,
                                 OutboxPublisherService outboxPublisherService,
                                 TelemetryService telemetryService,
                                 ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.idempotencyService = idempotencyService;
        this.outboxPublisherService = outboxPublisherService;
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Core Mutation Engine (Step 4 of Event Lifecycle):
     * Executes inside an atomic local ACID transaction on Oracle XE:
     * 1. Acquires @Lock(LockModeType.PESSIMISTIC_WRITE) on the Account row.
     * 2. Validates balance and currency compatibility.
     * 3. Updates the Account balance.
     * 4. Inserts TRANSACTION record.
     * 5. Synchronously inserts AUDIT_LOG for immediate security non-repudiation.
     * 6. Inserts OUTBOX_EVENT for Kafka propagation.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public MutationResponse mutateBalance(MutationRequest request, String username) {
        long startTimeNanos = System.nanoTime();

        // 1. In-Memory Idempotency Matrix Check (<5ms)
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            MutationResponse cachedResponse = idempotencyService.checkIdempotency(request.getIdempotencyKey());
            if (cachedResponse != null) {
                telemetryService.recordMutation(true, System.nanoTime() - startTimeNanos);
                return cachedResponse;
            }
        }

        // 2. Pessimistic Row Lock on Source Account (SELECT ... FOR UPDATE)
        long lockStart = System.nanoTime();
        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + request.getAccountId() + " was not found."));
        telemetryService.recordLockWait(System.nanoTime() - lockStart);

        // 3. Currency Validation
        if (request.getCurrency() != null && !account.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new CurrencyMismatchException("Account currency (" + account.getCurrency() + ") does not match transaction currency (" + request.getCurrency() + ").");
        }

        BigDecimal beforeBalance = account.getCurrentBalance();
        BigDecimal mutationAmount = request.getMutationAmount();
        BigDecimal afterBalance;
        String operation = request.getOperation().toUpperCase();

        // 4. Concurrency Guard: Strict Overdraft Prevention
        if ("DEBIT".equals(operation)) {
            if (beforeBalance.compareTo(mutationAmount) < 0) {
                // Record failed transaction attempt
                String failedRef = "TX-FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                TransactionRecord failedTx = new TransactionRecord(
                        account.getAccountId(),
                        request.getTargetAccountId(),
                        mutationAmount,
                        account.getCurrency(),
                        account.getCurrency(),
                        request.getTransactionType(),
                        failedRef,
                        "FAILED",
                        "INSUFFICIENT_FUNDS"
                );
                transactionRepository.save(failedTx);
                throw new InsufficientFundsException("Insufficient account balance. Available: ₱" + beforeBalance.toPlainString() + ", Requested: ₱" + mutationAmount.toPlainString());
            }
            afterBalance = beforeBalance.subtract(mutationAmount);
        } else if ("CREDIT".equals(operation)) {
            afterBalance = beforeBalance.add(mutationAmount);
        } else {
            throw new IllegalArgumentException("Unsupported mutation operation: " + operation);
        }

        // 5. Update Account Balance
        account.setCurrentBalance(afterBalance);
        accountRepository.save(account);

        // 6. Insert TRANSACTION Record
        String referenceNo = "TX-PH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        TransactionRecord txRecord = new TransactionRecord(
                account.getAccountId(),
                request.getTargetAccountId(),
                mutationAmount,
                account.getCurrency(),
                account.getCurrency(),
                request.getTransactionType(),
                referenceNo,
                "SUCCESS",
                null
        );
        txRecord = transactionRepository.save(txRecord);

        // 7. Synchronously Insert AUDIT_LOG in Oracle XE (Security & Operational Activity Audit)
        AuditLog auditLog = new AuditLog(
                account.getCustomerId(),
                "BALANCE_MUTATION_" + operation,
                "ACCOUNT",
                String.format("User [%s] initiated %s of ₱%s on Account [%s]. Balance updated from ₱%s to ₱%s. Ref: %s",
                        username != null ? username : "anonymous", operation, mutationAmount.toPlainString(),
                        account.getAccountNumber(), beforeBalance.toPlainString(), afterBalance.toPlainString(), referenceNo)
        );
        auditLogRepository.save(auditLog);

        // 8. Insert OUTBOX_EVENT in Oracle XE (Transactional Outbox Pattern)
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(new OutboxPayload(
                    txRecord.getTransactionId(),
                    referenceNo,
                    account.getAccountId(),
                    account.getCustomerId(),
                    operation,
                    mutationAmount,
                    account.getCurrency(),
                    beforeBalance,
                    afterBalance,
                    LocalDateTime.now().toString()
            ));
        } catch (Exception ex) {
            payloadJson = String.format("{\"transactionId\":%d,\"referenceNo\":\"%s\",\"amount\":\"%s\"}",
                    txRecord.getTransactionId(), referenceNo, mutationAmount.toPlainString());
        }

        OutboxEvent outboxEvent = new OutboxEvent(txRecord.getTransactionId(), "TRANSACTION_SUCCESS", payloadJson);
        outboxEventRepository.save(outboxEvent);

        // 9. Build Response Object
        MutationResponse response = new MutationResponse(
                txRecord.getTransactionId(),
                referenceNo,
                account.getAccountId(),
                account.getAccountNumber(),
                operation,
                mutationAmount,
                account.getCurrency(),
                beforeBalance,
                afterBalance,
                "SUCCESS",
                false
        );

        // 10. Cache Response in Redis Idempotency Matrix (TTL: 24h)
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            idempotencyService.saveIdempotency(request.getIdempotencyKey(), response, Duration.ofHours(24));
        }

        // 11. Async Notification to Kafka Outbox Publisher
        outboxPublisherService.triggerImmediatePublish(outboxEvent);

        telemetryService.recordMutation(false, System.nanoTime() - startTimeNanos);
        return response;
    }

    public static class OutboxPayload {
        public Long transactionId;
        public String referenceNo;
        public Long accountId;
        public Long customerId;
        public String operation;
        public BigDecimal amount;
        public String currency;
        public BigDecimal beforeBalance;
        public BigDecimal afterBalance;
        public String timestamp;

        public OutboxPayload() {}

        public OutboxPayload(Long transactionId, String referenceNo, Long accountId, Long customerId,
                             String operation, BigDecimal amount, String currency, BigDecimal beforeBalance,
                             BigDecimal afterBalance, String timestamp) {
            this.transactionId = transactionId;
            this.referenceNo = referenceNo;
            this.accountId = accountId;
            this.customerId = customerId;
            this.operation = operation;
            this.amount = amount;
            this.currency = currency;
            this.beforeBalance = beforeBalance;
            this.afterBalance = afterBalance;
            this.timestamp = timestamp;
        }
    }
}
