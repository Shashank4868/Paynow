package com.personal.payment.kafka;

import com.personal.payment.Configuration.PaymentKafkaProperties;
import com.personal.payment.kafka.event.PaymentCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;
    private final PaymentKafkaProperties kafkaProperties;

    public PaymentEventPublisher(
            KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate,
            PaymentKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public void publish(PaymentCreatedEvent paymentCreatedEvent) {
        kafkaTemplate.send(
                kafkaProperties.createdTopic(),
                paymentCreatedEvent.paymentId().toString(),
                paymentCreatedEvent
        );
    }


}
