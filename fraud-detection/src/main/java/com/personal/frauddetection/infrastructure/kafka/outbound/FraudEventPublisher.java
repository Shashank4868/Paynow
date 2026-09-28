package com.personal.frauddetection.infrastructure.kafka.outbound;

import com.personal.frauddetection.application.FraudEvaluationService;
import com.personal.frauddetection.dto.FraudDecisionEvent;
import com.personal.frauddetection.dto.NotificationEvent;
import com.personal.payment.event.PaymentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FraudEventPublisher {

    private static final String NOTIFICATION_TOPIC = "notification-kafka-topic";

    private static final Logger log = LoggerFactory.getLogger(FraudEventPublisher.class);
    private final FraudEvaluationService fraudEvaluationService;

    private final KafkaTemplate<String, FraudDecisionEvent> paymentKafkaTemplate;
    private final KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate;

    public FraudEventPublisher(FraudEvaluationService fraudEvaluationService, KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate, KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate) {
        this.fraudEvaluationService = fraudEvaluationService;
        this.paymentKafkaTemplate = kafkaTemplate;
        this.notificationKafkaTemplate = notificationKafkaTemplate;
    }

    public void publish(PaymentCreatedEvent event, String topic) {
        log.debug("Publishing fraud decision for payment {} to {}", event.paymentId(), topic);
        FraudDecisionEvent fraudDecisionEvent = fraudEvaluationService.evaluate(event);
        paymentKafkaTemplate.send(topic, event.paymentId().toString(), fraudDecisionEvent);
        notificationKafkaTemplate.send(NOTIFICATION_TOPIC, new NotificationEvent(
                event.receiverEmail(),
                event.senderEmail(),
                fraudDecisionEvent.reason(),
                fraudDecisionEvent.decision()
        ));
    }
}
