package com.personal.payment.event;

import java.time.Instant;
import java.util.UUID;

public record FraudDecisionEvent(
        UUID paymentId,
        String decision,
        String reason,
        Instant checkedAt
) {
}