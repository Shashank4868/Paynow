package com.personal.frauddetection.infrastructure.kafka.outbound;

import com.personal.frauddetection.application.FraudEvaluationService;
import com.personal.frauddetection.dto.FraudDecisionEvent;
import com.personal.payment.event.PaymentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FraudEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FraudEventPublisher.class);
    private final FraudEvaluationService fraudEvaluationService;

    private final KafkaTemplate<String, FraudDecisionEvent> paymentKafkaTemplate;

    public FraudEventPublisher(FraudEvaluationService fraudEvaluationService, KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate) {
        this.fraudEvaluationService = fraudEvaluationService;
        this.paymentKafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentCreatedEvent event, String topic) {
        log.debug("Publishing fraud decision for payment {} to {}", event.paymentId(), topic);
        FraudDecisionEvent fraudDecisionEvent = fraudEvaluationService.evaluate(event);
        paymentKafkaTemplate.send(topic, event.paymentId().toString(), fraudDecisionEvent);
    }
}
