package com.retailforge.checkout.service;

public class PaymentTimeoutException extends RuntimeException {
    public PaymentTimeoutException(String reason) {
        super(reason);
    }
}
