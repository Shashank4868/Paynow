package com.personal.frauddetection.infrastructure.kafka.inbound;

import com.personal.frauddetection.application.FraudEvaluationService;
import com.personal.frauddetection.infrastructure.kafka.outbound.FraudEventPublisher;
import com.personal.payment.event.PaymentCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCreatedListener {

    private static final String PAYMENT_DECISION_TOPIC = "payment.decision";

    private final FraudEvaluationService fraudEvaluationService;
    private final FraudEventPublisher eventPublisher;

    public PaymentCreatedListener(
            FraudEvaluationService fraudEvaluationService,
            FraudEventPublisher eventPublisher) {
        this.fraudEvaluationService = fraudEvaluationService;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
            topics = "payment.created",
            groupId = "fraud-detection"
    )
    public void consume(PaymentCreatedEvent event) {
        eventPublisher.publish(
                fraudEvaluationService.evaluate(event),
                PAYMENT_DECISION_TOPIC
        );
    }
}
