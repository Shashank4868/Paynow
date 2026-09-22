package com.personal.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentNotFoundExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<String> handlePaymentNotFound(PaymentNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exception.getMessage());
    }
}
