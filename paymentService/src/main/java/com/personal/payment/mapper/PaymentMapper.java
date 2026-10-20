package com.personal.payment.mapper;

import com.personal.payment.dto.PaymentResponse;
import com.personal.payment.entity.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    /**
     * Builds a new payment. The sender always comes from the authenticated user's token,
     * never from the request body.
     */
    public Payment toPayment(CreatePaymentRequest request, String senderId, String senderEmail) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request must not be null");
        }
        return Payment.builder()
                .senderId(senderId)
                .senderEmail(senderEmail)
                .receiverId(request.receiverId())
                .receiverEmail(request.receiverEmail())
                .amount(request.amount())
                .currency(request.currency())
                .idempotencyKey(request.idempotencyKey())
                .reference(request.reference())
                .status(PaymentStatus.PENDING)
                .build();
    }

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment must not be null");
        }
        return PaymentResponse.builder()
                .id(payment.getId())
                .senderId(payment.getSenderId())
                .receiverId(payment.getReceiverId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .idempotencyKey(payment.getIdempotencyKey())
                .reference(payment.getReference())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
