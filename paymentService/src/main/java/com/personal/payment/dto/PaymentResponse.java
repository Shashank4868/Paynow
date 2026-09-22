package com.personal.payment.dto;

import com.personal.payment.entity.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentResponse(
        UUID id,
        String senderId,
        String receiverId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String idempotencyKey,
        String reference,
        Instant createdAt,
        Instant updatedAt
) {
}