package com.personal.payment.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Body of POST /payments/create-payment.
 * There is deliberately no senderId/senderEmail: the sender is the signed-in user (JWT).
 */
public record CreatePaymentRequest(

        @NotBlank(message = "Receiver ID is required")
        String receiverId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code, e.g. INR")
        String currency,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Receiver email must be valid"
        )
        @NotBlank(message = "Receiver email is required")
        String receiverEmail,

        String reference
) {
}
