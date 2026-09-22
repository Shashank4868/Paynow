package com.personal.payment.kafka;

import com.personal.payment.kafka.event.FraudDecisionEvent;
import com.personal.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FraudDecisionListener {

    private final PaymentService paymentService;

    public FraudDecisionListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "${payment.kafka.decision-topic}",
            groupId = "${payment.kafka.consumer-group-id}"
    )
    public void listen(FraudDecisionEvent event) {
        switch (event.decision()) {
            case APPROVED -> paymentService.approvePayment(event.paymentId());
            case FLAGGED -> paymentService.flagPayment(event.paymentId(), event.reason());
        }

    }
}
