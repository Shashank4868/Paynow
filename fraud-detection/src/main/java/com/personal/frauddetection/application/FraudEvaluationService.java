package com.personal.frauddetection.application;

import com.personal.frauddetection.dto.FraudDecisionEvent;
import com.personal.frauddetection.entity.FraudDecision;
import com.personal.payment.event.PaymentCreatedEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class FraudEvaluationService {

    private static final BigDecimal APPROVAL_LIMIT = new BigDecimal("1000000");

//    TODO
    public FraudDecisionEvent evaluate(PaymentCreatedEvent event) {
        if (event.amount().compareTo(APPROVAL_LIMIT) < 0) {
            return new FraudDecisionEvent(
                    event.paymentId(),
                    FraudDecision.APPROVED,
                    "Passed initial fraud check rules",
                    Instant.now()
            );
        }

        return new FraudDecisionEvent(
                event.paymentId(),
                FraudDecision.FLAGGED,
                "Failed initial fraud check rules",
                Instant.now()
        );
    }
}
