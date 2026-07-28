package com.retailforge.pricing;

import com.retailforge.pricing.service.PricingService;
import com.retailforge.pricing.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PricingServiceTest {

    @Autowired
    PricingService pricing;
    @Autowired
    PromotionService promotions;

    @Test
    void percentCouponApplies() {
        var quote = pricing.calculate(List.of(new PricingService.Line("prod-1", new BigDecimal("100.00"), 1)), "SAVE10");
        assertEquals(new BigDecimal("100.00"), quote.subtotal());
        assertEquals(new BigDecimal("10.00"), quote.discount());
    }

    @Test
    void promotionsDoNotStack() {
        // both SAVE10 and BOGO could match but only the best single discount applies
        var quote = pricing.calculate(List.of(new PricingService.Line("prod-coffee", new BigDecimal("12.50"), 4)), null);
        assertEquals(new BigDecimal("25.00"), quote.discount());
    }

    @Test
    void expiredCouponIsInvalid() {
        assertFalse(promotions.validate("SPRING20").valid());
        assertTrue(promotions.validate("SAVE10").valid());
    }
}
