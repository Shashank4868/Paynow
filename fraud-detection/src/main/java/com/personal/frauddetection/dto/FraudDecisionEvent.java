package com.personal.frauddetection.dto;

import com.personal.frauddetection.entity.FraudDecision;

import java.time.Instant;
import java.util.UUID;

public record FraudDecisionEvent(
        UUID paymentId,
        FraudDecision decision,
        String reason,
        Instant checkedAt
) {
}