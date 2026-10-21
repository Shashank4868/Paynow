package com.personal.payment.kafka;

import com.personal.payment.entity.Payment;
import com.personal.payment.enums.PaymentStatus;
import com.personal.payment.exception.InvalidStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies status changes to a payment, enforcing the rules in {@link PaymentStatus#allowedNext()}.
 */
@Component
public class PaymentDecisionHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentDecisionHandler.class);

    public void approve(Payment payment) {
        transition(payment, PaymentStatus.COMPLETED, null);
    }

    public void flag(Payment payment, String reason) {
        transition(payment, PaymentStatus.FLAGGED, reason);
    }

    /**
     * Moves the payment to {@code target}.
     * Re-applying the status it already has is a no-op, so a redelivered Kafka event
     * doesn't throw and get retried forever.
     */
    boolean transition(Payment payment, PaymentStatus target, String reason) {
        PaymentStatus current = payment.getStatus();

        if (current == target) {
            log.info("Payment {} is already {}; ignoring duplicate decision", payment.getId(), target);
            return false;
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(
                    "Payment " + payment.getId() + " cannot move from " + current + " to " + target);
        }

        payment.setStatus(target);
        if (reason != null) {
            payment.setFailureReason(reason);
        }
        return true;
    }
}
