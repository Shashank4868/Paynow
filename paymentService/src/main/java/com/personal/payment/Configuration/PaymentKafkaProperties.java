package com.personal.payment.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.kafka")
public record PaymentKafkaProperties(
        String createdTopic,
        String decisionTopic,
        String consumerGroupId
) {
}
