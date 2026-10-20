package com.personal.payment.service;

import com.personal.payment.entity.Payment;
import com.personal.payment.enums.PaymentStatus;
import com.personal.payment.kafka.event.NotificationEvent;
import com.personal.payment.repo.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Runs against the real database: the notification must go out only after the status change commits.
 * Kafka is mocked, so nothing is actually published.
 */
@SpringBootTest
class PaymentNotificationAfterCommitTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // Wildcard type so the mock replaces the auto-configured KafkaTemplate<?, ?> instead of being added
    // next to it; otherwise the real template would publish to Kafka during the test.
    @MockitoBean
    private KafkaTemplate<?, ?> mockKafkaTemplate;

    private UUID paymentId;

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, Object> kafkaTemplate() {
        return (KafkaTemplate<String, Object>) mockKafkaTemplate;
    }

    @BeforeEach
    void createPendingPayment() {
        paymentId = paymentRepository.saveAndFlush(Payment.builder()
                .senderId("after-commit-test-sender").receiverId("after-commit-test-receiver")
                .senderEmail("sender@example.com").receiverEmail("receiver@example.com")
                .amount(BigDecimal.TEN).currency("INR").status(PaymentStatus.PENDING)
                .idempotencyKey("act-" + UUID.randomUUID())
                .build()).getId();
    }

    @AfterEach
    void deletePayment() {
        if (paymentId != null) {
            paymentRepository.deleteById(paymentId);
        }
    }

    @Test
    void rolledBackDecisionSendsNoNotification() {
        transactionTemplate.executeWithoutResult(tx -> {
            paymentService.approvePayment(paymentId);
            tx.setRollbackOnly();
        });

        verify(kafkaTemplate(), never()).send(eq("notification-kafka-topic"), anyString(), any());
        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void committedDecisionSendsOneNotification() {
        paymentService.approvePayment(paymentId);

        verify(kafkaTemplate(), times(1)).send("notification-kafka-topic", paymentId.toString(),
                new NotificationEvent("receiver@example.com", "sender@example.com", "Payment completed", "APPROVED"));

        paymentService.approvePayment(paymentId);
        verify(kafkaTemplate(), times(1)).send(eq("notification-kafka-topic"), anyString(), any());
    }
}
