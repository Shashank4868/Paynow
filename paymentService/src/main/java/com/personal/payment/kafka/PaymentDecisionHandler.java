package com.personal.payment.kafka;

import com.personal.payment.entity.Payment;
import com.personal.payment.entity.PaymentStatus;
import com.personal.payment.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;


@Component
public class PaymentDecisionHandler {

    public void approve(Payment payment) {
        PaymentStatus status = payment.getStatus();
        if (status == PaymentStatus.COMPLETED ||
                status == PaymentStatus.CANCELLED) {
            throw new InvalidStateTransitionException(
                    "Payment cannot be approved from status " + status
            );
        }
        status = PaymentStatus.COMPLETED;
        payment.setStatus(status);
    }

    public void flag(Payment payment, String reason) {

        PaymentStatus status = payment.getStatus();
        if (status == PaymentStatus.COMPLETED ||
                status == PaymentStatus.CANCELLED) {
            throw new InvalidStateTransitionException(
                    "Payment cannot be flagged from status " + status
            );
        }
        status = PaymentStatus.FLAGGED;
        payment.setStatus(status);
        payment.setFailureReason(reason);
    }
}
