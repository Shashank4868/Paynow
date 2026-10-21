package com.personal.payment.controller;

import com.personal.payment.dto.PaymentResponse;
import com.personal.payment.entity.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.mapper.PaymentMapper;
import com.personal.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Payment payment = paymentService.getPaymentForUser(id, jwt.getSubject());
        return paymentMapper.toResponse(payment);
    }

    @PostMapping("/create-payment")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String senderEmail = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(senderEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Access token must contain an email claim");
        }

        Payment payment = paymentService.createPayment(request, jwt.getSubject(), senderEmail);

        return ResponseEntity
                .created(URI.create("/payments/" + payment.getId()))
                .body(paymentMapper.toResponse(payment));
    }
}
