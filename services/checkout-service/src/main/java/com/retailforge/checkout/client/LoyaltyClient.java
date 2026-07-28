package com.retailforge.checkout.client;

public interface LoyaltyClient {
    void award(String customerId, String transactionId, long points);
}
