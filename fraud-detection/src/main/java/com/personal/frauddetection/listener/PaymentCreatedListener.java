package com.personal.frauddetection.listener;

import com.personal.frauddetection.event.FraudEventPublisher;
import com.personal.payment.event.PaymentCreatedEvent;
import com.personal.payment.event.FraudDecisionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class PaymentCreatedListener {

    private final FraudEventPublisher  eventPublisher;

    private static final String PAYMENT_DECISION_TOPIC = "payment.decision";

    public PaymentCreatedListener(FraudEventPublisher  eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
            topics = "payment.created",
            groupId = "fraud-detection"
    )
    public void consume(PaymentCreatedEvent event) {

        // Next: run fraud rules and publish payment.approved / payment.rejected.

        if(event.amount().compareTo( new BigDecimal("1000000")) < 0 ) {
            eventPublisher.publishPayment(
            new FraudDecisionEvent(
                event.paymentId(),
                "APPROVED",
                "Passed initial fraud check rules",
                Instant.now()
        )
            , PAYMENT_DECISION_TOPIC);
        } else {
            eventPublisher.publishPayment(
            new FraudDecisionEvent(
                event.paymentId(),
                "FLAGGED",
                "Failed initial fraud check rules",
                Instant.now()
        )
            , PAYMENT_DECISION_TOPIC);
        }
    }

}
