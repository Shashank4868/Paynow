package com.personal.payment.kafka;

import com.personal.payment.Configuration.PaymentKafkaProperties;
import com.personal.payment.kafka.event.NotificationEvent;
import com.personal.payment.kafka.event.PaymentStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the notification for a payment status change, but only once the transaction that saved
 * the new status has committed. If the save fails or rolls back, nothing is sent.
 */
@Component
public class PaymentNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationPublisher.class);

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final PaymentKafkaProperties kafkaProperties;

    public PaymentNotificationPublisher(
            KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            PaymentKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(PaymentStatusChangedEvent event) {
        NotificationEvent notification = toNotification(event);
        if (notification == null) {
            return;
        }
        kafkaTemplate.send(kafkaProperties.notificationTopic(), event.paymentId().toString(), notification);
        log.info("Notification queued for payment {} ({})", event.paymentId(), event.status());
    }

    /**
     * Picks who is emailed: a completed payment goes to the receiver; a flagged one goes only to
     * the sender (it is under review and may never arrive, so the receiver is not told).
     * Only the statuses a fraud decision produces are announced; anything else returns null.
     */
    static NotificationEvent toNotification(PaymentStatusChangedEvent event) {
        return switch (event.status()) {
            case COMPLETED -> new NotificationEvent(
                    event.receiverEmail(), event.senderEmail(), "Payment completed", "APPROVED");
            case FLAGGED -> new NotificationEvent(
                    event.senderEmail(), event.senderEmail(),
                    event.reason() != null ? event.reason() : "Payment under review", "FLAGGED");
            default -> null;
        };
    }
}
