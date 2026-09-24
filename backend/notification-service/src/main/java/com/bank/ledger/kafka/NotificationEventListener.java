package com.bank.ledger.kafka;

import com.bank.ledger.service.NotificationConsumerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationConsumerService notificationConsumerService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationConsumerService notificationConsumerService,
                                     ObjectMapper objectMapper) {
        this.notificationConsumerService = notificationConsumerService;
        this.objectMapper = objectMapper;
    }

    /**
     * Requirement B5: Consumer Group 3 - Customer Real-Time Alerts & Notification Gateway
     */
    @KafkaListener(topics = "transaction-events", groupId = "ledger-notification-group", concurrency = "3")
    public void onNotificationEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            Long customerId = node.has("customerId") ? node.get("customerId").asLong() : 1L;
            Long accountId = node.has("accountId") ? node.get("accountId").asLong() : 1L;
            String operation = node.has("operation") ? node.get("operation").asText() : "DEBIT";
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            String refNo = node.has("referenceNo") ? node.get("referenceNo").asText() : "TX-REF";

            notificationConsumerService.consumeNotificationEvent(customerId, accountId, operation, amount, refNo);
        } catch (Exception ex) {
            log.error("Failed to process notification event: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}
