package com.bank.notification.service;

import com.bank.notification.model.Notification;
import com.bank.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationKafkaConsumer(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ledger.transaction.events", groupId = "notification-service-group")
    @Transactional
    public void consume(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            Long customerId  = node.has("customerId")    ? node.get("customerId").asLong()    : 1L;
            Long accountId   = node.has("accountId")     ? node.get("accountId").asLong()     : 0L;
            String operation = node.has("operation")     ? node.get("operation").asText()     : "DEBIT";
            String amount    = node.has("amount")        ? node.get("amount").asText()        : "0";
            String refNo     = node.has("referenceNo")   ? node.get("referenceNo").asText()   : "N/A";

            String alertMsg = String.format(
                    "PayPink Alert: %s%s has been %sed on Account ID %d. Ref: %s. (Philippine Banking Network)",
                    "\u20B1", amount, operation.toLowerCase(), accountId, refNo);

            notificationRepository.save(new Notification(customerId, alertMsg, "SENT"));
            log.info("[notification-service] Notification saved for customer {} ref {}", customerId, refNo);

        } catch (Exception e) {
            log.error("[notification-service] Failed to process notification event: {}", e.getMessage());
        }
    }
}
