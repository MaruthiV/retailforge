package com.retailforge.loyalty.event;

import com.retailforge.common.event.DomainEvent;
import com.retailforge.loyalty.service.LoyaltyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// consumes TransactionCompleted and awards points. earn() is idempotent so replays are safe.
@Component
public class LoyaltyEventHandler {
    private static final Logger log = LoggerFactory.getLogger(LoyaltyEventHandler.class);

    private final LoyaltyService loyalty;

    public LoyaltyEventHandler(LoyaltyService loyalty) {
        this.loyalty = loyalty;
    }

    public void onTransactionCompleted(DomainEvent event) {
        String customerId = (String) event.payload().get("customerId");
        if (customerId == null) {
            log.info("transaction {} has no customer, no points", event.transactionId());
            return;
        }
        long points = pointsFor(event.payload().get("total"));
        loyalty.earn(customerId, event.transactionId(), points);
    }

    private long pointsFor(Object total) {
        if (total == null) return 0;
        return new BigDecimal(total.toString()).longValue();
    }
}
