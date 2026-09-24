package com.bank.ledger.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    public static final String TRANSACTION_EVENTS_DLT_TOPIC = "transaction-events.DLT";

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionEventsDltTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_DLT_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
