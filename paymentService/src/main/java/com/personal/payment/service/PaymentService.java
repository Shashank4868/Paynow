package com.personal.payment.service;

import com.personal.payment.dto.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.exception.DuplicateIdempotencyKeyException;
import com.personal.payment.exception.PaymentNotFoundException;
import com.personal.payment.kafka.PaymentDecisionHandler;
import com.personal.payment.kafka.PaymentEventPublisher;
import com.personal.payment.kafka.event.PaymentCreatedEvent;
import com.personal.payment.mapper.PaymentMapper;
import com.personal.payment.repo.PaymentRepository;
import jakarta.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentMapper paymentMapper;
    private final PaymentDecisionHandler paymentDecisionHandler;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher paymentEventPublisher, PaymentMapper paymentMapper, PaymentDecisionHandler paymentDecisionHandler) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.paymentMapper = paymentMapper;
        this.paymentDecisionHandler = paymentDecisionHandler;
    }

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        if (paymentRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateIdempotencyKeyException(
                    "Idempotency key already exists: " + request.idempotencyKey());
        }

        try {
            Payment newPayment = paymentMapper.toPayment(request);
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
                            savedPayment.getCreatedAt(),
                            savedPayment.getSenderEmail(),
                            savedPayment.getReceiverEmail()
                    )
            );


            return savedPayment;

        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyKeyConstraintViolation(exception)) {
                throw new DuplicateIdempotencyKeyException(
                        "Idempotency key already exists: " + request.idempotencyKey());
            }
            throw exception;
        }
    }

    private boolean isIdempotencyKeyConstraintViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_payment_idempotency_key".equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public ResponseEntity<?> getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    @Transactional
    public void approvePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        paymentDecisionHandler.approve(payment);
        paymentRepository.save(payment);
    }

    @Transactional
    public void flagPayment(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        paymentDecisionHandler.flag(payment, reason);
        paymentRepository.save(payment);
    }
}
