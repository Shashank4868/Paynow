package com.personal.payment.kafka;

import com.personal.payment.entity.Payment;
import com.personal.payment.enums.PaymentStatus;
import com.personal.payment.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentDecisionHandlerTest {

    private final PaymentDecisionHandler handler = new PaymentDecisionHandler();

    private static Payment withStatus(PaymentStatus status) {
        return Payment.builder().status(status).build();
    }

    @Test
    void pendingPaymentIsCompletedWhenApproved() {
        Payment payment = withStatus(PaymentStatus.PENDING);
        handler.approve(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void pendingPaymentIsFlaggedWithReason() {
        Payment payment = withStatus(PaymentStatus.PENDING);
        handler.flag(payment, "Amount over limit");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FLAGGED);
        assertThat(payment.getFailureReason()).isEqualTo("Amount over limit");
    }

    @Test
    void flaggedPaymentCannotBeApproved() {
        Payment payment = withStatus(PaymentStatus.FLAGGED);
        assertThatThrownBy(() -> handler.approve(payment))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FLAGGED);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"FAILED", "CANCELLED"})
    void terminalPaymentsRejectDecisions(PaymentStatus terminal) {
        assertThatThrownBy(() -> handler.approve(withStatus(terminal)))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> handler.flag(withStatus(terminal), "x"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void completedPaymentCannotBeFlagged() {
        assertThatThrownBy(() -> handler.flag(withStatus(PaymentStatus.COMPLETED), "late"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void duplicateDecisionIsIgnored() {
        Payment completed = withStatus(PaymentStatus.COMPLETED);
        handler.approve(completed);
        assertThat(completed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        Payment flagged = withStatus(PaymentStatus.FLAGGED);
        flagged.setFailureReason("first");
        handler.flag(flagged, "second");
        assertThat(flagged.getFailureReason()).isEqualTo("first");
    }

    @Test
    void terminalStatusesHaveNoNextStatus() {
        assertThat(PaymentStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(PaymentStatus.FAILED.isTerminal()).isTrue();
        assertThat(PaymentStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(PaymentStatus.PENDING.isTerminal()).isFalse();
    }
}
