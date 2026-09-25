package com.bank.notification.consumer;

import com.bank.notification.model.Notification;
import com.bank.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ledger.transaction.events", groupId = "notification-group")
    public void consumeTransactionEvent(String payload) {
        try {
            log.info("[notification-service] Received Kafka transaction event: {}", payload);
            JsonNode node = objectMapper.readTree(payload);

            Long customerId = node.has("customerId") ? node.get("customerId").asLong() : 1L;
            String operation = node.has("operation") ? node.get("operation").asText() : "MUTATION";
            String amount = node.has("amount") ? node.get("amount").asText() : "0.00";
            String refNo = node.has("referenceNo") ? node.get("referenceNo").asText() : "UNKNOWN";

            String message = String.format("PayPink Alert: ₱%s %s operation successful. Ref: %s", amount, operation, refNo);

            Notification notification = new Notification(customerId, message, "SENT");
            notificationRepository.save(notification);
            log.info("[notification-service] Created customer notification id={} for customer={}", notification.getNotificationId(), customerId);
        } catch (Exception e) {
            log.error("[notification-service] Failed to process transaction event: {}", e.getMessage());
        }
    }
}
