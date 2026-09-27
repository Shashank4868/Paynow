package com.personal.payment.mapper;

import com.personal.payment.dto.CreatePaymentRequest;
import com.personal.payment.dto.PaymentResponse;
import com.personal.payment.entity.Payment;
import com.personal.payment.entity.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public Payment toPayment(CreatePaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request must not be null");
        }
        return Payment.builder()
                .senderId(request.senderId())
                .receiverId(request.receiverId())
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
                .senderId(payment.getSenderId())
                .receiverId(payment.getReceiverId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .reference(payment.getReference())
                .status(payment.getStatus())
                .build();
    }
}
