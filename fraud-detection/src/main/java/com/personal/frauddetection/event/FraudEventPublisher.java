package com.personal.frauddetection.event;

import com.personal.payment.event.FraudDecisionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
public class FraudEventPublisher {

    private final KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate;


    public FraudEventPublisher(KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }



    public void publishPayment(FraudDecisionEvent event, String topic) {
        System.out.println(event);
        kafkaTemplate.send(
                topic,
                event.paymentId().toString(),
                event
        );
    }

}
