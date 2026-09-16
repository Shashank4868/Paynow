package com.personal.payment.controller;

import com.personal.payment.dto.CreatePaymentRequest;
import com.personal.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/payments/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable String id) {
        return paymentService.getPaymentById(id);
    }

    @PostMapping("/create-payment")
    public ResponseEntity<?> CreatePayment(@Valid @RequestBody CreatePaymentRequest payment) {
        return paymentService.CreatePayment(payment);
    }

}
