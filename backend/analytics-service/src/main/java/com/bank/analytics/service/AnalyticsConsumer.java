package com.bank.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);

    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalDebitCount = new AtomicLong(0);
    private final AtomicLong totalCreditCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, BigDecimal> volumeByCurrency = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public AnalyticsConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ledger.transaction.events", groupId = "analytics-group")
    public void consumeTransactionEvent(String payload) {
        try {
            log.info("[analytics-service] Processing Kafka transaction event: {}", payload);
            JsonNode node = objectMapper.readTree(payload);

            totalEventsProcessed.incrementAndGet();
            String operation = node.has("operation") ? node.get("operation").asText() : "UNKNOWN";
            if ("DEBIT".equalsIgnoreCase(operation)) {
                totalDebitCount.incrementAndGet();
            } else if ("CREDIT".equalsIgnoreCase(operation)) {
                totalCreditCount.incrementAndGet();
            }

            String currency = node.has("currency") ? node.get("currency").asText() : "PHP";
            BigDecimal amount = node.has("amount") ? new BigDecimal(node.get("amount").asText()) : BigDecimal.ZERO;

            volumeByCurrency.merge(currency, amount, BigDecimal::add);
            log.info("[analytics-service] Total Events: {}, Total Volume: {}", totalEventsProcessed.get(), volumeByCurrency);
        } catch (Exception e) {
            log.error("[analytics-service] Error analyzing transaction event: {}", e.getMessage());
        }
    }

    public Map<String, Object> getMetricsSummary() {
        return Map.of(
                "totalEventsProcessed", totalEventsProcessed.get(),
                "totalDebitCount", totalDebitCount.get(),
                "totalCreditCount", totalCreditCount.get(),
                "volumeByCurrency", volumeByCurrency
        );
    }
}
