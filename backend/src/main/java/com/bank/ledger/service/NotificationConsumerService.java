package com.bank.ledger.service;

import com.bank.ledger.model.postgres.Notification;
import com.bank.ledger.repository.postgres.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class NotificationConsumerService {

    private final NotificationRepository notificationRepository;

    public NotificationConsumerService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Kafka Consumer for Notification Topic (Reversed Flow):
     * 1. Receive Event
     * 2. Send Notification (Simulated Gateway dispatch)
     * 3. Persist Notification Status to PostgreSQL (SENT / FAILED / RETRY)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification consumeNotificationEvent(Long customerId, Long accountId, String operation,
                                                 BigDecimal amount, String referenceNo) {
        String message = String.format("PayPink Alert: ₱%,.4f has been %sed on Account ID %d. Ref: %s. (Philippine Banking Network)",
                amount, operation.toLowerCase(), accountId, referenceNo);

        // Step 2: Send Notification via Gateway
        String deliveryStatus = dispatchAlertToExternalGateway(customerId, message);

        // Step 3: Persist Notification Status in PostgreSQL
        Notification notification = new Notification(customerId, message, deliveryStatus);
        return notificationRepository.save(notification);
    }

    private String dispatchAlertToExternalGateway(Long customerId, String message) {
        try {
            // Simulated digital SMS/Email gateway dispatch
            return "SENT";
        } catch (Exception ex) {
            return "RETRY";
        }
    }
}
