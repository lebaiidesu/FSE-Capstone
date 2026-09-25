package com.bank.audit.consumer;

import com.bank.audit.model.LedgerMutationAudit;
import com.bank.audit.repository.LedgerMutationAuditRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final LedgerMutationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AuditConsumer(LedgerMutationAuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ledger.transaction.events", groupId = "audit-group")
    public void consumeTransactionEvent(String payload) {
        try {
            log.info("[audit-service] Received Kafka transaction event: {}", payload);
            JsonNode node = objectMapper.readTree(payload);

            Long txId = node.get("transactionId").asLong();
            Long accId = node.get("accountId").asLong();
            String operation = node.get("operation").asText();
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            String currency = node.has("currency") ? node.get("currency").asText() : "PHP";
            BigDecimal beforeBalance = new BigDecimal(node.get("beforeBalance").asText());
            BigDecimal afterBalance = new BigDecimal(node.get("afterBalance").asText());

            LedgerMutationAudit audit = new LedgerMutationAudit(txId, accId, operation, amount, currency, beforeBalance, afterBalance);
            auditRepository.save(audit);
            log.info("[audit-service] Immutable audit entry written for transactionId={} auditId={}", txId, audit.getAuditId());
        } catch (Exception e) {
            log.error("[audit-service] Failed to persist audit entry: {}", e.getMessage());
        }
    }
}
