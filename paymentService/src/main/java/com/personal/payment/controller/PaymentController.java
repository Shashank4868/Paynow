package com.personal.payment.controller;

import com.personal.payment.dto.CreatePaymentRequest;
import com.personal.payment.entity.Payment;
import com.personal.payment.mapper.PaymentMapper;
import com.personal.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> getPaymentById(@PathVariable UUID id) {
        return paymentService.getPaymentById(id);
    }

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentMapper.toResponse(payment));
    }

}
