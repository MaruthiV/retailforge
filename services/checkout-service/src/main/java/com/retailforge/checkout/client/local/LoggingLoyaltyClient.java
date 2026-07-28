package com.retailforge.checkout.client.local;

import com.retailforge.checkout.client.LoyaltyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!distributed")
public class LoggingLoyaltyClient implements LoyaltyClient {
    private static final Logger log = LoggerFactory.getLogger(LoggingLoyaltyClient.class);

    @Override
    public void award(String customerId, String transactionId, long points) {
        log.info("award {} points to customer={} txn={}", points, customerId, transactionId);
    }
}
