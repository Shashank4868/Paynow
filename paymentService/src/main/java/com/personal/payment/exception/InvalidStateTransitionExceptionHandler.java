package com.personal.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InvalidStateTransitionExceptionHandler {

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<String> handleInvalidStateTransition(
            InvalidStateTransitionException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(exception.getMessage());
    }
}
