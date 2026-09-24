package com.bank.ledger.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(@Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes mutation event payload to Kafka topic transaction-events with accountId partitioning key.
     * Synchronously awaits broker ACK before completing the outbox transaction.
     */
    public boolean publishTransactionEvent(Long accountId, String payload) {
        if (kafkaTemplate == null) {
            log.debug("KafkaTemplate not configured; skipping Kafka publish.");
            return true;
        }

        try {
            String partitionKey = accountId != null ? String.valueOf(accountId) : "0";
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(KafkaTopicConfig.TRANSACTION_EVENTS_TOPIC, partitionKey, payload);
            
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
            log.info("Successfully published to Kafka topic {} partition {} offset {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;
        } catch (Exception ex) {
            log.error("Failed to publish transaction event to Kafka: {}", ex.getMessage(), ex);
            throw new RuntimeException("Kafka Broker Ack Timeout / Connection Failed: " + ex.getMessage(), ex);
        }
    }
}
