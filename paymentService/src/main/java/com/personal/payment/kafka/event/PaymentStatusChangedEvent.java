package com.personal.payment.kafka.event;

import com.personal.payment.enums.PaymentStatus;

import java.util.UUID;

/**
 * In-process Spring event raised when a fraud decision actually changes a payment's status.
 * It is turned into a Kafka notification only after the transaction that changed the status commits.
 */
public record PaymentStatusChangedEvent(
        UUID paymentId,
        PaymentStatus status,
        String senderEmail,
        String receiverEmail,
        String reason
) {
}
