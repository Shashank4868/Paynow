package com.personal.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotBlank(message = "Sender ID is required")
        String senderId,

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

        String reference
) {}