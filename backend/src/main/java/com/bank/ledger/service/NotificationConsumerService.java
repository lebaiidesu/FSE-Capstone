package com.bank.ledger.service;

import com.bank.ledger.model.postgres.Notification;
import com.bank.ledger.repository.postgres.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class NotificationConsumerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumerService.class);
    private final NotificationRepository notificationRepository;

    public NotificationConsumerService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
 * Kafka consumer for notifications:
 * 1. Claim: insert a PENDING row (UNIQUE reference_no). Only one delivery can win.
 * 2. Dispatch: send the alert via the gateway (only the winner reaches here).
 * 3. Persist the final status: SENT / FAILED / RETRY.
 *
 * No method-level @Transactional: each repository call commits on its own
 * (Postgres transaction manager), so a duplicate insert fails cleanly.
 */
public Notification consumeNotificationEvent(Long customerId, Long accountId, String operation,
                                             BigDecimal amount, String referenceNo) {
    if (referenceNo == null || referenceNo.isBlank()) {
        throw new IllegalArgumentException("Event is missing referenceNo; cannot deduplicate notification.");
    }

    // Fast path for normal redeliveries
    if (notificationRepository.existsByReferenceNo(referenceNo)) {
        log.info("Notification for referenceNo={} already claimed. Skipping duplicate.", referenceNo);
        return null;
    }

    String message = String.format("PayPink Alert: ₱%,.4f has been %sed on Account ID %d. Ref: %s.",
            amount, operation != null ? operation.toLowerCase() : "mutat", accountId, referenceNo);

    // Step 1: Claim (race-safe; the UNIQUE constraint decides the winner)
    Notification notification;
    try {
        notification = notificationRepository.saveAndFlush(
                new Notification(customerId, referenceNo, message, "PENDING"));
    } catch (DataIntegrityViolationException dup) {
        log.info("Concurrent duplicate for referenceNo={}. Another consumer claimed it.", referenceNo);
        return null;
    }

    // Step 2: Dispatch (only the claimant sends)
    String deliveryStatus = dispatchAlertToExternalGateway(customerId, message);

    // Step 3: Persist final status
    notification.setStatus(deliveryStatus);
    notification.setUpdatedDate(LocalDateTime.now());
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
