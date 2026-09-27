package com.personal.notification.dto;

import com.personal.notification.entity.FraudDecision;

public record NotificationEvent (
        String recipient,
        String sender,
        String subject,
        FraudDecision message
) {}
