package com.bank.ledger.service;

import com.bank.ledger.model.postgres.Notification;
import com.bank.ledger.repository.postgres.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class NotificationConsumerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumerService.class);
    private final NotificationRepository notificationRepository;

    public NotificationConsumerService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Kafka Consumer for Notification Topic (Reversed Flow):
     * 1. Receive Event
     * 2. Idempotency check on referenceNo
     * 3. Send Notification (Simulated Gateway dispatch)
     * 4. Persist Notification Status to PostgreSQL (SENT / FAILED / RETRY)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification consumeNotificationEvent(Long customerId, Long accountId, String operation,
                                                 BigDecimal amount, String referenceNo) {
        if (referenceNo != null && !referenceNo.isBlank() && notificationRepository.existsByReferenceNo(referenceNo)) {
            log.info("Notification for referenceNo={} already dispatched. Skipping duplicate.", referenceNo);
            return null;
        }

        String message = String.format("PayPink Alert: ₱%,.4f has been %sed on Account ID %d. Ref: %s. (Philippine Banking Network)",
                amount, operation != null ? operation.toLowerCase() : "mutat", accountId, referenceNo);

        // Step 2: Send Notification via Gateway
        String deliveryStatus = dispatchAlertToExternalGateway(customerId, message);

        // Step 3: Persist Notification Status in PostgreSQL
        Notification notification = new Notification(customerId, referenceNo, message, deliveryStatus);
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
