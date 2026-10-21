package com.personal.payment.kafka;

import com.personal.payment.Configuration.PaymentKafkaProperties;
import com.personal.payment.kafka.event.PaymentCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends {@link PaymentCreatedEvent} to fraud-detection, but only once the transaction that saved the payment
 * has committed. If the save fails or rolls back, nothing is sent.
 */
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(PaymentCreatedEvent paymentCreatedEvent) {
        kafkaTemplate.send(
                kafkaProperties.createdTopic(),
                paymentCreatedEvent.paymentId().toString(),
                paymentCreatedEvent
        );
    }


}
