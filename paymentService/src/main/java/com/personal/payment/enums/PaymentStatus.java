package com.personal.payment.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Payment lifecycle. Every allowed move is listed in {@link #allowedNext()};
 * anything not listed is rejected by {@code PaymentDecisionHandler}.
 *
 * <pre>
 * PENDING ──► COMPLETED   (fraud check approved)
 *    │ ├────► FLAGGED     (fraud check flagged; needs review, never auto-approved)
 *    │ ├────► FAILED
 *    │ ├────► CANCELLED
 *    └──────► PROCESSING  (reserved for the ledger step: money is moving)
 * PROCESSING ► COMPLETED | FAILED
 * FLAGGED ───► FAILED | CANCELLED
 * COMPLETED, FAILED, CANCELLED are terminal.
 * </pre>
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    FLAGGED,
    CANCELLED;

    public Set<PaymentStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(PROCESSING, COMPLETED, FLAGGED, FAILED, CANCELLED);
            case PROCESSING -> EnumSet.of(COMPLETED, FAILED);
            case FLAGGED -> EnumSet.of(FAILED, CANCELLED);
            case COMPLETED, FAILED, CANCELLED -> EnumSet.noneOf(PaymentStatus.class);
        };
    }

    public boolean canTransitionTo(PaymentStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
}
