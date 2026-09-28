package com.personal.frauddetection.dto;

import com.personal.frauddetection.entity.FraudDecision;

public record NotificationEvent(
        String recipient,
        String sender,
        String subject,
        FraudDecision message
) {
}