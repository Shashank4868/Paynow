package com.personal.payment.service;

import com.personal.payment.dto.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.entity.PaymentStatus;
import com.personal.payment.event.PaymentCreatedEvent;
import com.personal.payment.event.PaymentEventPublisher;
import com.personal.payment.repo.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    public ResponseEntity<?> CreatePayment(CreatePaymentRequest request) {
        Payment existingPayment = paymentRepository
                .findByIdempotencyKey(request.idempotencyKey())
                .orElse(null);

        if (existingPayment != null) {
            return ResponseEntity.ok(existingPayment);
        }

        try {
            Payment newPayment = Payment.builder()
                    .senderId(request.senderId())
                    .receiverId(request.receiverId())
                    .amount(request.amount())
                    .currency(request.currency())
                    .idempotencyKey(request.idempotencyKey())
                    .reference(request.reference())
                    .status(PaymentStatus.PENDING)
                    .build();

            System.out.println("Payment Initiated");
            Payment savedPayment = paymentRepository.saveAndFlush(newPayment);
            System.out.println("Payment Saved Successfully");
            paymentEventPublisher.publish(
                    new PaymentCreatedEvent(
                            savedPayment.getId(),
                            savedPayment.getSenderId(),
                            savedPayment.getReceiverId(),
                            savedPayment.getAmount(),
                            savedPayment.getCurrency(),
                            savedPayment.getCreatedAt()
                    )
            );


            return ResponseEntity.status(HttpStatus.CREATED).body(savedPayment);

        } catch (DataIntegrityViolationException exception) {
            return paymentRepository
                    .findByIdempotencyKey(request.idempotencyKey())
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to create payment"));
        }
    }

    public ResponseEntity<?> getPaymentById(String id) {
        try {
            return paymentRepository.findById(UUID.fromString(id))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body("Invalid Payment ID");
        }
    }


    //    TODO
    public ResponseEntity<?> processPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElse(null);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found");
        }
        try {
            Payment updatedPayment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Payment Updated");
    }
}
