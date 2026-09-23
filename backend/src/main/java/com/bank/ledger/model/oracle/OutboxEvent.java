package com.bank.ledger.model.oracle;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OUTBOX_EVENT")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // 'TRANSACTION_SUCCESS', 'TRANSACTION_FAILED'

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // 'PENDING', 'PROCESSED', 'FAILED'

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    public OutboxEvent() {}

    public OutboxEvent(Long transactionId, String eventType, String payload) {
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.createdDate = LocalDateTime.now();
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDateTime processedDate) { this.processedDate = processedDate; }
}
