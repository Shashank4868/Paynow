package com.personal.frauddetection.infrastructure.kafka.outbound;

import com.personal.frauddetection.dto.FraudDecisionEvent;
import com.personal.frauddetection.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FraudEventPublisher {

    private static final String NOTIFICATION_TOPIC = "notification-kafka-topic";

    private static final Logger log = LoggerFactory.getLogger(FraudEventPublisher.class);

    private final KafkaTemplate<String, FraudDecisionEvent> paymentKafkaTemplate;
    private final KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate;

    public FraudEventPublisher(KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate,  KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate) {
        this.paymentKafkaTemplate = kafkaTemplate;
        this.notificationKafkaTemplate = notificationKafkaTemplate;
    }

    public void publish(FraudDecisionEvent event, String topic) {
        log.debug("Publishing fraud decision for payment {} to {}", event.paymentId(), topic);
        paymentKafkaTemplate.send(topic, event.paymentId().toString(), event);
        notificationKafkaTemplate.send(NOTIFICATION_TOPIC, new NotificationEvent(
                "rolickjaiswal@gmail.com",
                event.reason(),
                event.decision()
        ));
    }
}
