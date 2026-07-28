package com.retailforge.checkout;

import com.retailforge.checkout.client.LoyaltyClient;
import com.retailforge.checkout.domain.Cart;
import com.retailforge.checkout.domain.Transaction;
import com.retailforge.checkout.service.CartService;
import com.retailforge.checkout.service.CheckoutService;
import com.retailforge.checkout.service.PaymentDeclinedException;
import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.EventType;
import com.retailforge.common.event.RecordingEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CheckoutFlowTest {

    static final AtomicInteger AWARDS = new AtomicInteger();

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        EventPublisher recording() {
            return new RecordingEventPublisher();
        }

        @Bean
        @Primary
        LoyaltyClient recordingLoyalty() {
            return (customerId, transactionId, points) -> AWARDS.incrementAndGet();
        }
    }

    @Autowired
    CartService carts;
    @Autowired
    CheckoutService checkout;
    @Autowired
    EventPublisher events;

    private Cart seededCart(String coupon) {
        Cart cart = carts.create("store-001", "cust-001");
        carts.addItem(cart.getId(), "prod-1", "Widget", new BigDecimal("100.00"), 1);
        return cart;
    }

    @Test
    void appliesDiscountBeforeTax() {
        AWARDS.set(0);
        Cart cart = seededCart("SAVE10");
        Transaction txn = checkout.checkout(cart.getId(), "4111111111111111", "SAVE10");
        assertEquals(new BigDecimal("100.00"), txn.getSubtotal());
        assertEquals(new BigDecimal("10.00"), txn.getDiscount());
        assertEquals(new BigDecimal("7.65"), txn.getTax());
        assertEquals(new BigDecimal("97.65"), txn.getTotal());
    }

    @Test
    void retryingCheckoutDoesNotAwardLoyaltyTwice() {
        AWARDS.set(0);
        ((RecordingEventPublisher) events).clear();
        Cart cart = seededCart(null);
        Transaction first = checkout.checkout(cart.getId(), "4111111111111111", null);
        Transaction retry = checkout.checkout(cart.getId(), "4111111111111111", null);
        assertEquals(first.getId(), retry.getId());
        assertEquals(1, AWARDS.get());
        assertEquals(1, ((RecordingEventPublisher) events).countByType(EventType.TRANSACTION_COMPLETED));
    }

    @Test
    void declinedPaymentThrows() {
        AWARDS.set(0);
        Cart cart = seededCart(null);
        assertThrows(PaymentDeclinedException.class,
                () -> checkout.checkout(cart.getId(), "4000000000000002", null));
    }
}
