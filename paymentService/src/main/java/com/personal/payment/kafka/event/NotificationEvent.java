package com.personal.payment.kafka.event;

/**
 * Message on the notification topic. Matches the notification service's NotificationEvent:
 * {@code message} is its FraudDecision enum name ("APPROVED" or "FLAGGED").
 */
public record NotificationEvent(
        String recipient,
        String sender,
        String subject,
        String message
) {
}
