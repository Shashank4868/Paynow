package com.personal.frauddetection.dto;

import com.personal.frauddetection.entity.FraudDecision;

public record NotificationEvent(
        String recipient,
        String subject,
        FraudDecision message
) {
}