package com.personal.frauddetection.infrastructure.kafka.outbound;

import com.personal.frauddetection.application.FraudEvaluationService;
import com.personal.frauddetection.config.FraudKafkaProperties;
import com.personal.frauddetection.dto.FraudDecisionEvent;
import com.personal.payment.event.PaymentCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FraudEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void decisionIsSentToTheConfiguredTopic() {
        KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate = mock(KafkaTemplate.class);
        FraudEventPublisher publisher = new FraudEventPublisher(
                new FraudEvaluationService(),
                kafkaTemplate,
                new FraudKafkaProperties("custom.created", "custom.decision", "custom-group"));
        UUID paymentId = UUID.randomUUID();

        publisher.publish(new PaymentCreatedEvent(paymentId, "s", "r", BigDecimal.TEN, "INR",
                Instant.now(), "s@example.com", "r@example.com"));

        verify(kafkaTemplate).send(eq("custom.decision"), eq(paymentId.toString()), any(FraudDecisionEvent.class));
    }
}
