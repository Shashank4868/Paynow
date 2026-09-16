package com.personal.payment.event;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PaymentEventPublisher {

    public static final String PAYMENT_CREATED_TOPIC = "payment.created";
    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentCreatedEvent paymentCreatedEvent) {
        kafkaTemplate.send(
                PAYMENT_CREATED_TOPIC,
                paymentCreatedEvent.paymentId().toString(),
                paymentCreatedEvent
        );
        System.out.println("Inside Payment created event sent to topic: " + paymentCreatedEvent.paymentId().toString());
    }

    @KafkaListener(
            topics = "payment.decision",
            groupId = "process-payment"
    )
    public void listen(FraudDecisionEvent fraudDecisionEvent) {
        if (Objects.equals(fraudDecisionEvent.decision(), "APPROVED")) {
            System.out.println("Approved payment");
        } else if (Objects.equals(fraudDecisionEvent.decision(), "FLAGGED")) {
            System.out.println("Flagged payment");
        }
    }
}
