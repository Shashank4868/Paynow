package com.personal.payment.kafka.event;

import com.personal.payment.entity.FraudDecision;

import java.time.Instant;
import java.util.UUID;

public record FraudDecisionEvent(
        UUID paymentId,
        FraudDecision decision,
        String reason,
        Instant checkedAt
) {
}