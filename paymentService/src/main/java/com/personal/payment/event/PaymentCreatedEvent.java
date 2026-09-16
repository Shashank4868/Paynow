package com.personal.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreatedEvent(
        UUID paymentId,
        String senderId,
        String receiverId,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
}
