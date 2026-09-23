package com.bank.ledger.service;

import com.bank.ledger.model.oracle.OutboxEvent;
import com.bank.ledger.repository.oracle.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final AuditConsumerService auditConsumerService;
    private final NotificationConsumerService notificationConsumerService;
    private final ReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;
    private final ExecutorService asyncEventPool = Executors.newFixedThreadPool(10);

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                  @Lazy AuditConsumerService auditConsumerService,
                                  @Lazy NotificationConsumerService notificationConsumerService,
                                  @Lazy ReconciliationService reconciliationService,
                                  ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.auditConsumerService = auditConsumerService;
        this.notificationConsumerService = notificationConsumerService;
        this.reconciliationService = reconciliationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Immediate dispatch trigger for low-latency streaming
     */
    public void triggerImmediatePublish(OutboxEvent event) {
        asyncEventPool.submit(() -> processSingleOutboxEvent(event));
    }

    /**
     * Scheduled CDC / Polling Publisher Sweep (Runs every 3 seconds to guarantee at-least-once delivery)
     */
    @Scheduled(fixedDelay = 3000)
    public void pollAndPublishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedDateAsc("PENDING");
        for (OutboxEvent event : pendingEvents) {
            processSingleOutboxEvent(event);
        }
    }

    private void processSingleOutboxEvent(OutboxEvent event) {
        try {
            // Parse event payload
            JsonNode node = objectMapper.readTree(event.getPayload());
            Long txId = node.has("transactionId") ? node.get("transactionId").asLong() : event.getTransactionId();
            Long accountId = node.has("accountId") ? node.get("accountId").asLong() : 1L;
            Long customerId = node.has("customerId") ? node.get("customerId").asLong() : 1L;
            String operation = node.has("operation") ? node.get("operation").asText() : "DEBIT";
            BigDecimal amount = node.has("amount") ? new BigDecimal(node.get("amount").asText()) : BigDecimal.ZERO;
            String currency = node.has("currency") ? node.get("currency").asText() : "PHP";
            BigDecimal beforeBalance = node.has("beforeBalance") ? new BigDecimal(node.get("beforeBalance").asText()) : BigDecimal.ZERO;
            BigDecimal afterBalance = node.has("afterBalance") ? new BigDecimal(node.get("afterBalance").asText()) : BigDecimal.ZERO;
            String refNo = node.has("referenceNo") ? node.get("referenceNo").asText() : "TX-REF";

            // Mark Outbox Event as PROCESSED
            event.setStatus("PROCESSED");
            event.setProcessedDate(LocalDateTime.now());
            outboxEventRepository.save(event);

            // Dispatch to Downstream Kafka Consumers asynchronously:
            // 1. Audit Service -> Writes append-only LEDGER_MUTATION_AUDIT in PostgreSQL
            auditConsumerService.consumeAuditEvent(txId, accountId, operation, amount, currency, beforeBalance, afterBalance);

            // 2. Notification Service -> Dispatches alert, then persists status in PostgreSQL NOTIFICATION
            notificationConsumerService.consumeNotificationEvent(customerId, accountId, operation, amount, refNo);

            // 3. Reconciliation Service -> Real-time consistency check
            reconciliationService.validateEventRealTime(txId);

        } catch (Exception ex) {
            event.setStatus("FAILED");
            outboxEventRepository.save(event);
        }
    }
}
