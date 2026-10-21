package com.personal.payment.kafka;

import com.personal.payment.enums.PaymentStatus;
import com.personal.payment.kafka.event.NotificationEvent;
import com.personal.payment.kafka.event.PaymentStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentNotificationPublisherTest {

    private static PaymentStatusChangedEvent changedTo(PaymentStatus status, String reason) {
        return new PaymentStatusChangedEvent(UUID.randomUUID(), status, "sender@example.com", "receiver@example.com", reason);
    }

    @Test
    void completedPaymentIsAnnouncedAsApproved() {
        NotificationEvent notification = PaymentNotificationPublisher.toNotification(changedTo(PaymentStatus.COMPLETED, null));

        assertThat(notification).isEqualTo(new NotificationEvent(
                "receiver@example.com", "sender@example.com", "Payment completed", "APPROVED"));
    }

    @Test
    void flaggedPaymentGoesOnlyToTheSenderWithTheFraudReason() {
        NotificationEvent notification = PaymentNotificationPublisher.toNotification(changedTo(PaymentStatus.FLAGGED, "Amount over limit"));

        assertThat(notification.recipient()).isEqualTo("sender@example.com");
        assertThat(notification.message()).isEqualTo("FLAGGED");
        assertThat(notification.subject()).isEqualTo("Amount over limit");
    }

    @Test
    void otherStatusesAreNotAnnounced() {
        assertThat(PaymentNotificationPublisher.toNotification(changedTo(PaymentStatus.FAILED, null))).isNull();
    }
}
