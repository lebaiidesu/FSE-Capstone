package com.bank.ledger.kafka;

import com.bank.ledger.service.AuditConsumerService;
import com.bank.ledger.service.ReconciliationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AuditAndReconEventListeners {

    private static final Logger log = LoggerFactory.getLogger(AuditAndReconEventListeners.class);

    private final AuditConsumerService auditConsumerService;
    private final ReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;

    public AuditAndReconEventListeners(AuditConsumerService auditConsumerService,
                                       ReconciliationService reconciliationService,
                                       ObjectMapper objectMapper) {
        this.auditConsumerService = auditConsumerService;
        this.reconciliationService = reconciliationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Requirement B4: Consumer Group 1 - Immutable PostgreSQL Financial Audit
     */
    @KafkaListener(topics = "transaction-events", groupId = "ledger-audit-group", concurrency = "3")
    public void onAuditTransactionEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
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
                String operation = node.has("operation") ? node.get("operation").asText() : "DEBIT";
                BigDecimal amount = new BigDecimal(node.get("amount").asText());
                String currency = node.has("currency") ? node.get("currency").asText() : "PHP";
                BigDecimal beforeBalance = new BigDecimal(node.get("beforeBalance").asText());
                BigDecimal afterBalance = new BigDecimal(node.get("afterBalance").asText());

                auditConsumerService.consumeAuditEvent(txId, accountId, operation, amount, currency, beforeBalance, afterBalance);
            }
        } catch (Exception ex) {
            log.error("Failed to consume audit event: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Requirement B4: Consumer Group 2 - Real-Time Ledger Reconciliation Listener
     */
    @KafkaListener(topics = "transaction-events", groupId = "ledger-recon-group", concurrency = "3")
    public void onReconciliationEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            Long txId = node.get("transactionId").asLong();
            reconciliationService.validateEventRealTime(txId);
        } catch (Exception ex) {
            log.error("Failed to process real-time reconciliation event: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}
