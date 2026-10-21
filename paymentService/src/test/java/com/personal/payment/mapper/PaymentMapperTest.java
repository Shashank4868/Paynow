package com.personal.payment.mapper;

import com.personal.payment.dto.PaymentResponse;
import com.personal.payment.entity.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void senderComesFromArgumentsNotRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "receiver-1", new BigDecimal("10.00"), "INR", "key-1", "r@example.com", "ref");

        Payment payment = mapper.toPayment(request, "keycloak-sub", "me@example.com");

        assertThat(payment.getSenderId()).isEqualTo("keycloak-sub");
        assertThat(payment.getSenderEmail()).isEqualTo("me@example.com");
        assertThat(payment.getReceiverId()).isEqualTo("receiver-1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void responseIncludesIdKeyAndTimestamps() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Payment payment = Payment.builder()
                .id(id).senderId("s").receiverId("r").amount(BigDecimal.ONE).currency("INR")
                .status(PaymentStatus.PENDING).idempotencyKey("key-1")
                .createdAt(now).updatedAt(now).senderEmail("s@example.com").build();

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.idempotencyKey()).isEqualTo("key-1");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }
}
