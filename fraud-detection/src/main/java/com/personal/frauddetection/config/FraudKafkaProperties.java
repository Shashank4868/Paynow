package com.personal.frauddetection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka names used by fraud-detection. The topics are read from the same environment variables
 * as paymentService (PAYMENT_CREATED_TOPIC, PAYMENT_DECISION_TOPIC), so both services stay in step.
 */
@ConfigurationProperties(prefix = "fraud.kafka")
public record FraudKafkaProperties(
        String createdTopic,
        String decisionTopic,
        String consumerGroupId
) {
}
