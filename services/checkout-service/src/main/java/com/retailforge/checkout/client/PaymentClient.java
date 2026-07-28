package com.retailforge.checkout.client;

import java.math.BigDecimal;

public interface PaymentClient {

    enum Status { APPROVED, DECLINED, TIMEOUT }

    record ChargeOutcome(Status status, String authCode, String reason) {}

    ChargeOutcome charge(String transactionId, BigDecimal amount, String card);
}
