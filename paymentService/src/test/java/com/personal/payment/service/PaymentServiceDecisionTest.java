package com.personal.payment.service;

import com.personal.payment.entity.Payment;
import com.personal.payment.enums.PaymentStatus;
import com.personal.payment.exception.InvalidStateTransitionException;
import com.personal.payment.kafka.PaymentDecisionHandler;
import com.personal.payment.kafka.event.PaymentStatusChangedEvent;
import com.personal.payment.mapper.PaymentMapper;
import com.personal.payment.repo.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceDecisionTest {

    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PaymentService service = new PaymentService(
            repository, new PaymentMapper(), new PaymentDecisionHandler(), events);

    private Payment stored(PaymentStatus status) {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID()).status(status)
                .senderEmail("sender@example.com").receiverEmail("receiver@example.com")
                .build();
        when(repository.findById(payment.getId())).thenReturn(Optional.of(payment));
        return payment;
    }

    @Test
    void approvingPendingPaymentRaisesStatusChangedEvent() {
        Payment payment = stored(PaymentStatus.PENDING);

        service.approvePayment(payment.getId());

        ArgumentCaptor<PaymentStatusChangedEvent> captor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(captor.getValue().receiverEmail()).isEqualTo("receiver@example.com");
    }

    @Test
    void flaggingCarriesTheReason() {
        Payment payment = stored(PaymentStatus.PENDING);

        service.flagPayment(payment.getId(), "Amount over limit");

        ArgumentCaptor<PaymentStatusChangedEvent> captor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.FLAGGED);
        assertThat(captor.getValue().reason()).isEqualTo("Amount over limit");
    }

    @Test
    void duplicateDecisionRaisesNothing() {
        Payment payment = stored(PaymentStatus.COMPLETED);

        service.approvePayment(payment.getId());

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void rejectedTransitionRaisesNothing() {
        Payment payment = stored(PaymentStatus.FLAGGED);

        assertThatThrownBy(() -> service.approvePayment(payment.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);
        verify(events, never()).publishEvent(any(Object.class));
    }
}
