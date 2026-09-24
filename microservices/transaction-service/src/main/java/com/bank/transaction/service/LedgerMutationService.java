package com.bank.transaction.service;

import com.bank.transaction.dto.MutationRequest;
import com.bank.transaction.dto.MutationResponse;
import com.bank.transaction.model.Account;
import com.bank.transaction.model.AuditLog;
import com.bank.transaction.model.OutboxEvent;
import com.bank.transaction.model.TransactionRecord;
import com.bank.transaction.repository.AccountRepository;
import com.bank.transaction.repository.AuditLogRepository;
import com.bank.transaction.repository.OutboxEventRepository;
import com.bank.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LedgerMutationService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Simple in-memory idempotency cache
    private final Map<String, MutationResponse> idempotencyCache = new ConcurrentHashMap<>();

    public LedgerMutationService(AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 AuditLogRepository auditLogRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MutationResponse mutateBalance(MutationRequest request, String username) {
        // 1. Idempotency check
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            MutationResponse cached = idempotencyCache.get(idempotencyKey);
            if (cached != null) {
                cached.setCachedIdempotentResponse(true);
                return cached;
            }
        }

        // 2. Pessimistic lock on account row
        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account with ID " + request.getAccountId() + " not found."));

        // 3. Currency validation
        if (request.getCurrency() != null && !account.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new RuntimeException("Currency mismatch: account=" + account.getCurrency()
                    + ", request=" + request.getCurrency());
        }

        BigDecimal beforeBalance = account.getCurrentBalance();
        BigDecimal amount = request.getMutationAmount();
        String operation = request.getOperation().toUpperCase();
        BigDecimal afterBalance;

        // 4. Balance mutation with overdraft guard
        if ("DEBIT".equals(operation)) {
            if (beforeBalance.compareTo(amount) < 0) {
                String failRef = "TX-FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                transactionRepository.save(new TransactionRecord(
                        account.getAccountId(), request.getTargetAccountId(), amount,
                        account.getCurrency(), account.getCurrency(),
                        request.getTransactionType(), failRef, "FAILED", "INSUFFICIENT_FUNDS"));
                throw new RuntimeException("Insufficient balance. Available: ₱"
                        + beforeBalance + ", Requested: ₱" + amount);
            }
            afterBalance = beforeBalance.subtract(amount);
        } else if ("CREDIT".equals(operation)) {
            afterBalance = beforeBalance.add(amount);
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + operation);
        }

        // 5. Persist updated balance
        account.setCurrentBalance(afterBalance);
        accountRepository.save(account);

        // 6. Insert TRANSACTION record
        String referenceNo = "TX-PH-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        TransactionRecord tx = transactionRepository.save(new TransactionRecord(
                account.getAccountId(), request.getTargetAccountId(), amount,
                account.getCurrency(), account.getCurrency(),
                request.getTransactionType(), referenceNo, "SUCCESS", null));

        // 7. Insert AUDIT_LOG in Oracle
        auditLogRepository.save(new AuditLog(account.getCustomerId(),
                "BALANCE_MUTATION_" + operation, "ACCOUNT",
                String.format("User [%s] %s ₱%s on Account [%s]. ₱%s → ₱%s. Ref: %s",
                        username, operation, amount, account.getAccountNumber(),
                        beforeBalance, afterBalance, referenceNo)));

        // 8. Insert OUTBOX_EVENT (Transactional Outbox Pattern)
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "transactionId", tx.getTransactionId(),
                    "referenceNo", referenceNo,
                    "accountId", account.getAccountId(),
                    "customerId", account.getCustomerId(),
                    "operation", operation,
                    "amount", amount.toPlainString(),
                    "currency", account.getCurrency(),
                    "beforeBalance", beforeBalance.toPlainString(),
                    "afterBalance", afterBalance.toPlainString(),
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            payload = "{\"transactionId\":" + tx.getTransactionId()
                    + ",\"referenceNo\":\"" + referenceNo + "\"}";
        }
        OutboxEvent outbox = outboxEventRepository.save(
                new OutboxEvent(tx.getTransactionId(), "TRANSACTION_SUCCESS", payload));

        // 9. Build response
        MutationResponse response = new MutationResponse(
                tx.getTransactionId(), referenceNo, account.getAccountId(),
                account.getAccountNumber(), operation, amount,
                account.getCurrency(), beforeBalance, afterBalance, "SUCCESS", false);

        // 10. Cache idempotency key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyCache.put(idempotencyKey, response);
        }

        // 11. Publish outbox event to Kafka immediately (best-effort)
        publishToKafka(outbox, payload);

        return response;
    }

    // Scheduled fallback poller for any PENDING outbox events missed
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollPendingOutboxEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedDateAsc("PENDING");
        for (OutboxEvent event : pending) {
            publishToKafka(event, event.getPayload());
        }
    }

    private void publishToKafka(OutboxEvent event, String payload) {
        try {
            kafkaTemplate.send("ledger.transaction.events",
                    String.valueOf(event.getTransactionId()), payload);
            event.setStatus("PROCESSED");
            event.setProcessedDate(LocalDateTime.now());
            outboxEventRepository.save(event);
        } catch (Exception e) {
            event.setStatus("FAILED");
            outboxEventRepository.save(event);
        }
    }
}
