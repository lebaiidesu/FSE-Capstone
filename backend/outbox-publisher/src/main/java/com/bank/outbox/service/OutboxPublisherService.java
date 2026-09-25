package com.bank.outbox.service;

import com.bank.outbox.model.OutboxEvent;
import com.bank.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);
    private static final String TOPIC = "ledger.transaction.events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                  KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndPublishOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedDateAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[outbox-publisher] Found {} pending outbox events to stream to Kafka", pendingEvents.size());
        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(TOPIC, String.valueOf(event.getTransactionId()), event.getPayload());
                event.setStatus("PROCESSED");
                event.setProcessedDate(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.info("[outbox-publisher] Successfully published outbox eventId={} txId={} to topic {}",
                        event.getEventId(), event.getTransactionId(), TOPIC);
            } catch (Exception e) {
                log.error("[outbox-publisher] Failed to publish outbox eventId={}: {}", event.getEventId(), e.getMessage());
                event.setStatus("FAILED");
                outboxEventRepository.save(event);
            }
        }
    }
}
