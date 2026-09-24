package com.bank.ledger.service;

import com.bank.ledger.model.oracle.OutboxEvent;
import com.bank.ledger.repository.oracle.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final AuditConsumerService auditConsumerService;
    private final NotificationConsumerService notificationConsumerService;
    private final ReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                  @Lazy AuditConsumerService auditConsumerService,
                                  @Lazy NotificationConsumerService notificationConsumerService,
                                  @Lazy ReconciliationService reconciliationService,
                                  ObjectMapper objectMapper,
                                  @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.auditConsumerService = auditConsumerService;
        this.notificationConsumerService = notificationConsumerService;
        this.reconciliationService = reconciliationService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Poller-Only Outbox Publisher Sweep (Runs every 1 second).
     * Claims up to 100 pending events using SKIP LOCKED.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollAndPublishPendingOutboxEvents() {
        List<OutboxEvent> claimedEvents = outboxEventRepository.claimBatch(
                LocalDateTime.now(), PageRequest.of(0, 100)
        );

        for (OutboxEvent event : claimedEvents) {
            try {
                publishEvent(event);
                event.setStatus("PROCESSED");
                event.setProcessedDate(LocalDateTime.now());
                event.setLastError(null);
            } catch (Exception ex) {
                int retry = event.getRetryCount() + 1;
                event.setRetryCount(retry);
                event.setLastError(ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 490)) : "Unknown Error");
                if (retry >= 5) {
                    event.setStatus("DEAD");
                    log.error("Outbox event ID {} marked DEAD after {} failed attempts: {}", event.getEventId(), retry, ex.getMessage());
                } else {
                    long backoffSec = Math.min((long) Math.pow(2, retry), 60);
                    event.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSec));
                    log.warn("Outbox event ID {} failed (attempt {}), retrying in {}s: {}", event.getEventId(), retry, backoffSec, ex.getMessage());
                }
            }
            outboxEventRepository.save(event);
        }
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        JsonNode node = objectMapper.readTree(event.getPayload());
        Long txId = node.has("transactionId") ? node.get("transactionId").asLong() : event.getTransactionId();
        Long accountId = node.has("accountId") ? node.get("accountId").asLong() : 1L;
        Long customerId = node.has("customerId") ? node.get("customerId").asLong() : 1L;
        String refNo = node.has("referenceNo") ? node.get("referenceNo").asText() : "TX-REF";

        // If Kafka is configured and active, publish to Kafka topic transaction-events
        if (kafkaTemplate != null) {
            kafkaTemplate.send("transaction-events", String.valueOf(accountId), event.getPayload()).get(5, TimeUnit.SECONDS);
        }

        // Process downstream consumer legs (in-process fallback or direct dispatch)
        if (node.has("entries") && node.get("entries").isArray()) {
            for (JsonNode entry : node.get("entries")) {
                Long legAccId = entry.get("accountId").asLong();
                String legType = entry.get("entryType").asText();
                BigDecimal legAmount = new BigDecimal(entry.get("amount").asText());
                String legCurr = entry.get("currency").asText();
                BigDecimal legBefore = new BigDecimal(entry.get("beforeBalance").asText());
                BigDecimal legAfter = new BigDecimal(entry.get("afterBalance").asText());

                auditConsumerService.consumeAuditEvent(txId, legAccId, legType, legAmount, legCurr, legBefore, legAfter);
            }
        } else {
            String operation = node.has("operation") ? node.get("operation").asText() : "DEBIT";
            BigDecimal amount = node.has("amount") ? new BigDecimal(node.get("amount").asText()) : BigDecimal.ZERO;
            String currency = node.has("currency") ? node.get("currency").asText() : "PHP";
            BigDecimal beforeBalance = node.has("beforeBalance") ? new BigDecimal(node.get("beforeBalance").asText()) : BigDecimal.ZERO;
            BigDecimal afterBalance = node.has("afterBalance") ? new BigDecimal(node.get("afterBalance").asText()) : BigDecimal.ZERO;

            auditConsumerService.consumeAuditEvent(txId, accountId, operation, amount, currency, beforeBalance, afterBalance);
        }

        String operation = node.has("operation") ? node.get("operation").asText() : "DEBIT";
        BigDecimal amount = node.has("amount") ? new BigDecimal(node.get("amount").asText()) : BigDecimal.ZERO;

        notificationConsumerService.consumeNotificationEvent(customerId, accountId, operation, amount, refNo);
        reconciliationService.validateEventRealTime(txId);
    }
}
