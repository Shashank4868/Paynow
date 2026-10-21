package com.personal.frauddetection.infrastructure.kafka.inbound;

import com.personal.frauddetection.infrastructure.kafka.outbound.FraudEventPublisher;
import com.personal.payment.event.PaymentCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCreatedListener {

    private final FraudEventPublisher eventPublisher;

    public PaymentCreatedListener(
            FraudEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
            topics = "${fraud.kafka.created-topic}",
            groupId = "${fraud.kafka.consumer-group-id}"
    )
    public void consume(PaymentCreatedEvent event) {
        eventPublisher.publish(event);
    }
}
