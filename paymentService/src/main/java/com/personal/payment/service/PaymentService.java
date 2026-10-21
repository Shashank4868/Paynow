package com.personal.payment.service;

import com.personal.payment.entity.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.enums.PaymentStatus;
import com.personal.payment.exception.DuplicateIdempotencyKeyException;
import com.personal.payment.exception.PaymentNotFoundException;
import com.personal.payment.kafka.PaymentDecisionHandler;
import com.personal.payment.kafka.event.PaymentCreatedEvent;
import com.personal.payment.kafka.event.PaymentStatusChangedEvent;
import com.personal.payment.mapper.PaymentMapper;
import com.personal.payment.repo.PaymentRepository;
import jakarta.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.function.Consumer;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentDecisionHandler paymentDecisionHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentMapper paymentMapper, PaymentDecisionHandler paymentDecisionHandler, ApplicationEventPublisher applicationEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.paymentDecisionHandler = paymentDecisionHandler;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Saves the payment and raises {@link PaymentCreatedEvent}. {@link com.personal.payment.kafka.PaymentEventPublisher}
     * sends it to Kafka only after this transaction commits, so fraud-detection never sees a rolled-back payment.
     *
     * @param senderId    the authenticated user's id (JWT "sub"), never taken from the request body
     * @param senderEmail the authenticated user's email (JWT "email")
     */
    @Transactional
    public Payment createPayment(CreatePaymentRequest request, String senderId, String senderEmail) {
        if (senderId.equals(request.receiverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a payment to yourself");
        }
        if (paymentRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateIdempotencyKeyException(
                    "Idempotency key already exists: " + request.idempotencyKey());
        }

        try {
            Payment newPayment = paymentMapper.toPayment(request, senderId, senderEmail);
            Payment savedPayment = paymentRepository.saveAndFlush(newPayment);
            log.info("Payment {} created by {}", savedPayment.getId(), senderId);

            applicationEventPublisher.publishEvent(
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

    /**
     * Returns the payment only if {@code userId} is its sender or receiver.
     * Anyone else gets a 404, so other people's payment ids can't be probed.
     */
    public Payment getPaymentForUser(UUID id, String userId) {
        return paymentRepository.findById(id)
                .filter(payment -> userId.equals(payment.getSenderId())
                        || userId.equals(payment.getReceiverId()))
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    @Transactional
    public void approvePayment(UUID paymentId) {
        applyDecision(paymentId, paymentDecisionHandler::approve);
    }

    @Transactional
    public void flagPayment(UUID paymentId, String reason) {
        applyDecision(paymentId, payment -> paymentDecisionHandler.flag(payment, reason));
    }

    /**
     * Applies a decision and, if it changed the status, raises {@link PaymentStatusChangedEvent}.
     * {@link com.personal.payment.kafka.PaymentNotificationPublisher} sends it only after this transaction commits,
     * and a duplicate decision (status unchanged) sends nothing.
     */
    private void applyDecision(UUID paymentId, Consumer<Payment> decision) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        PaymentStatus before = payment.getStatus();

        decision.accept(payment);
        paymentRepository.save(payment);

        if (payment.getStatus() != before) {
            applicationEventPublisher.publishEvent(new PaymentStatusChangedEvent(
                    payment.getId(),
                    payment.getStatus(),
                    payment.getSenderEmail(),
                    payment.getReceiverEmail(),
                    payment.getFailureReason()));
        }
    }
}
