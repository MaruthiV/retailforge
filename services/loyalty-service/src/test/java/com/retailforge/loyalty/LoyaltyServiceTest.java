package com.retailforge.loyalty;

import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.EventType;
import com.retailforge.common.event.RecordingEventPublisher;
import com.retailforge.loyalty.service.LoyaltyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LoyaltyServiceTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        EventPublisher recording() {
            return new RecordingEventPublisher();
        }
    }

    @Autowired
    LoyaltyService loyalty;

    @Autowired
    EventPublisher events;

    @Test
    void earnAddsPoints() {
        long before = loyalty.get("cust-002").getPointsBalance();
        loyalty.earn("cust-002", "txn-earn-1", 50);
        assertEquals(before + 50, loyalty.get("cust-002").getPointsBalance());
    }

    @Test
    void retryingCheckoutShouldNotAwardLoyaltyPointsTwice() {
        long before = loyalty.get("cust-001").getPointsBalance();
        loyalty.earn("cust-001", "txn-retry-9", 100);
        loyalty.earn("cust-001", "txn-retry-9", 100);
        assertEquals(before + 100, loyalty.get("cust-001").getPointsBalance());
        assertEquals(1, ((RecordingEventPublisher) events).byType(EventType.LOYALTY_POINTS_EARNED).size());
    }

    @Test
    void redeemFailsWhenInsufficient() {
        assertThrows(IllegalStateException.class, () -> loyalty.redeem("cust-002", 999999));
    }
}
