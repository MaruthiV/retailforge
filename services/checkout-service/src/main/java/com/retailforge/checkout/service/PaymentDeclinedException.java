package com.retailforge.checkout.service;

public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException(String reason) {
        super(reason);
    }
}
