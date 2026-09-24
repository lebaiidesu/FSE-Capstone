package com.bank.ledger.kafka;

import com.bank.ledger.service.AuditConsumerService;
import com.bank.ledger.service.NotificationConsumerService;
import com.bank.ledger.service.ReconciliationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionEventListeners {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListeners.class);

    private final AuditConsumerService auditConsumerService;
    private final NotificationConsumerService notificationConsumerService;
    private final ReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;

    public TransactionEventListeners(AuditConsumerService auditConsumerService,
                                     NotificationConsumerService notificationConsumerService,
                                     ReconciliationService reconciliationService,
                                     ObjectMapper objectMapper) {
        this.auditConsumerService = auditConsumerService;
        this.notificationConsumerService = notificationConsumerService;
        this.reconciliationService = reconciliationService;
        this.objectMapper = objectMapper;
    }

    /**
     * 1. Audit Consumer Group: Appends immutable financial ledger entries in PostgreSQL
     */
    @KafkaListener(topics = KafkaTopicConfig.TRANSACTION_EVENTS_TOPIC, groupId = "ledger-audit-group")
    public void onAudit(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long txId = node.get("transactionId").asLong();

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
                Long accountId = node.has("accountId") ? node.get("accountId").asLong() : 1L;
                String op = node.has("operation") ? node.get("operation").asText() : "DEBIT";
                BigDecimal amt = node.has("amount") ? new BigDecimal(node.get("amount").asText()) : BigDecimal.ZERO;
                String curr = node.has("currency") ? node.get("currency").asText() : "PHP";
                BigDecimal before = node.has("beforeBalance") ? new BigDecimal(node.get("beforeBalance").asText()) : BigDecimal.ZERO;
                BigDecimal after = node.has("afterBalance") ? new BigDecimal(node.get("afterBalance").asText()) : BigDecimal.ZERO;

                auditConsumerService.consumeAuditEvent(txId, accountId, op, amt, curr, before, after);
            }
        } catch (Exception ex) {
            log.error("Error processing audit Kafka event: {}", ex.getMessage(), ex);
            throw new RuntimeException("Audit consumption failed", ex);
        }
    }

    /**
     * 2. Notification Consumer Group: Dispatches customer alerts and logs delivery status in PostgreSQL
     */
    @KafkaListener(topics = KafkaTopicConfig.TRANSACTION_EVENTS_TOPIC, groupId = "ledger-notification-group")
    public void onNotify(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long customerId = node.has("customerId") ? node.get("customerId").asLong() : 1L;
            Long accountId = node.has("accountId") ? node.get("accountId").asLong() : 1L;
            String operation = node.has("operation") ? node.get("operation").asText() : "DEBIT";
            BigDecimal amount = node.has("amount") ? new BigDecimal(node.get("amount").asText()) : BigDecimal.ZERO;
            String refNo = node.has("referenceNo") ? node.get("referenceNo").asText() : "TX-REF";

            notificationConsumerService.consumeNotificationEvent(customerId, accountId, operation, amount, refNo);
        } catch (Exception ex) {
            log.error("Error processing notification Kafka event: {}", ex.getMessage(), ex);
            throw new RuntimeException("Notification consumption failed", ex);
        }
    }

    /**
     * 3. Reconciliation Consumer Group: Performs near-real-time dual-store consistency check
     */
    @KafkaListener(topics = KafkaTopicConfig.TRANSACTION_EVENTS_TOPIC, groupId = "ledger-recon-group")
    public void onRecon(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long txId = node.get("transactionId").asLong();
            reconciliationService.validateEventRealTime(txId);
        } catch (Exception ex) {
            log.error("Error processing reconciliation Kafka event: {}", ex.getMessage(), ex);
            throw new RuntimeException("Reconciliation real-time check failed", ex);
        }
    }
}
