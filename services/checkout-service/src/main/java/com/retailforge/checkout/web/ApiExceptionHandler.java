package com.retailforge.checkout.web;

import com.retailforge.checkout.service.PaymentDeclinedException;
import com.retailforge.checkout.service.PaymentTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> notFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    public ResponseEntity<Map<String, String>> declined(PaymentDeclinedException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("error", "payment_declined", "reason", e.getMessage()));
    }

    @ExceptionHandler(PaymentTimeoutException.class)
    public ResponseEntity<Map<String, String>> timeout(PaymentTimeoutException e) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of("error", "payment_timeout", "reason", e.getMessage()));
    }
}
