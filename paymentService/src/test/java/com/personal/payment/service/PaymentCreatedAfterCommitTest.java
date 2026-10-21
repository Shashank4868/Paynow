package com.personal.payment.service;

import com.personal.payment.entity.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.kafka.event.PaymentCreatedEvent;
import com.personal.payment.repo.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;

/**
 * Runs against the real database: payment.created must go out only after the new payment commits.
 * Kafka is mocked, so nothing is actually published.
 */
@SpringBootTest
class PaymentCreatedAfterCommitTest {

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

    private CreatePaymentRequest request(String idempotencyKey) {
        return new CreatePaymentRequest("created-test-receiver", BigDecimal.TEN, "INR",
                idempotencyKey, "receiver@example.com", null);
    }

    @AfterEach
    void deletePayment() {
        if (paymentId != null) {
            paymentRepository.deleteById(paymentId);
        }
    }

    @Test
    void rolledBackCreateSendsNoEvent() {
        String idempotencyKey = "pct-" + UUID.randomUUID();

        transactionTemplate.executeWithoutResult(tx -> {
            paymentService.createPayment(request(idempotencyKey), "created-test-sender", "sender@example.com");
            tx.setRollbackOnly();
        });

        verify(kafkaTemplate(), never()).send(eq("payment.created"), anyString(), any());
        assertThat(paymentRepository.existsByIdempotencyKey(idempotencyKey)).isFalse();
    }

    @Test
    void committedCreateSendsOneEvent() {
        Payment payment = paymentService.createPayment(
                request("pct-" + UUID.randomUUID()), "created-test-sender", "sender@example.com");
        paymentId = payment.getId();

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate()).send(eq("payment.created"), eq(paymentId.toString()), sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(PaymentCreatedEvent.class, event -> {
            assertThat(event.paymentId()).isEqualTo(paymentId);
            assertThat(event.senderEmail()).isEqualTo("sender@example.com");
            assertThat(event.receiverEmail()).isEqualTo("receiver@example.com");
        });
    }
}
