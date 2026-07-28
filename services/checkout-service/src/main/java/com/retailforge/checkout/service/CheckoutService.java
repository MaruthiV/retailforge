package com.retailforge.checkout.service;

import com.retailforge.checkout.client.LoyaltyClient;
import com.retailforge.checkout.client.PaymentClient;
import com.retailforge.checkout.client.PricingClient;
import com.retailforge.checkout.domain.Cart;
import com.retailforge.checkout.domain.CartItem;
import com.retailforge.checkout.domain.Transaction;
import com.retailforge.checkout.repo.CartItemRepository;
import com.retailforge.checkout.repo.CartRepository;
import com.retailforge.checkout.repo.TransactionRepository;
import com.retailforge.common.event.DomainEvent;
import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.EventType;
import com.retailforge.common.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    static final BigDecimal TAX_RATE = Money.of("8.5");

    private final CartRepository carts;
    private final CartItemRepository items;
    private final TransactionRepository transactions;
    private final PricingClient pricing;
    private final PaymentClient payment;
    private final LoyaltyClient loyalty;
    private final EventPublisher events;

    public CheckoutService(CartRepository carts, CartItemRepository items, TransactionRepository transactions,
                           PricingClient pricing, PaymentClient payment, LoyaltyClient loyalty, EventPublisher events) {
        this.carts = carts;
        this.items = items;
        this.transactions = transactions;
        this.pricing = pricing;
        this.payment = payment;
        this.loyalty = loyalty;
        this.events = events;
    }

    @Transactional
    public Transaction checkout(String cartId, String card, String coupon) {
        Cart cart = carts.findById(cartId).orElseThrow(() -> new IllegalArgumentException("unknown cart " + cartId));
        // idempotency: a retry of an already completed checkout returns the same transaction, no re-charge, no re-award
        if (cart.getStatus() == Cart.Status.CHECKED_OUT) {
            log.info("cart {} already checked out, returning existing transaction", cartId);
            return transactions.findFirstByCartId(cartId)
                    .orElseThrow(() -> new IllegalStateException("checked out cart has no transaction"));
        }
        if (cart.getStatus() == Cart.Status.CANCELLED) {
            throw new IllegalStateException("cart " + cartId + " is cancelled");
        }
        List<CartItem> lines = items.findByCartId(cartId);
        if (lines.isEmpty()) {
            throw new IllegalStateException("cannot checkout empty cart " + cartId);
        }

        events.publish("checkout-events", DomainEvent.of(EventType.CHECKOUT_STARTED, cartId, null, Map.of("cartId", cartId)));

        PricingClient.PricedCart priced = pricing.price(cartId, lines, coupon);
        BigDecimal taxable = Money.subtract(priced.subtotal(), priced.discount());
        BigDecimal tax = Money.percentOf(taxable, TAX_RATE);
        BigDecimal total = Money.add(taxable, tax);

        Transaction txn = transactions.save(
                new Transaction(cartId, cart.getCustomerId(), priced.subtotal(), priced.discount(), tax, total));

        PaymentClient.ChargeOutcome outcome = payment.charge(txn.getId(), total, card);
        switch (outcome.status()) {
            case APPROVED -> completeTransaction(cart, txn, outcome.authCode(), total);
            case DECLINED -> {
                txn.fail();
                transactions.save(txn);
                publishPaymentFailed(txn, "declined");
                throw new PaymentDeclinedException(outcome.reason());
            }
            case TIMEOUT -> {
                txn.fail();
                transactions.save(txn);
                publishPaymentFailed(txn, "timeout");
                throw new PaymentTimeoutException(outcome.reason());
            }
        }
        return txn;
    }

    private void completeTransaction(Cart cart, Transaction txn, String authCode, BigDecimal total) {
        txn.complete(authCode);
        cart.checkout();
        transactions.save(txn);
        carts.save(cart);
        events.publish("checkout-events", DomainEvent.keyed(
                "approved-" + txn.getId(), EventType.PAYMENT_APPROVED, txn.getId(), txn.getId(),
                Map.of("transactionId", txn.getId(), "total", total)));
        events.publish("checkout-events", DomainEvent.keyed(
                "completed-" + txn.getId(), EventType.TRANSACTION_COMPLETED, txn.getId(), txn.getId(),
                Map.of("transactionId", txn.getId(), "customerId", cart.getCustomerId(), "total", total)));
        loyalty.award(cart.getCustomerId(), txn.getId(), pointsFor(total));
    }

    private void publishPaymentFailed(Transaction txn, String reason) {
        events.publish("checkout-events", DomainEvent.of(EventType.PAYMENT_FAILED, txn.getId(), txn.getId(),
                Map.of("transactionId", txn.getId(), "reason", reason)));
    }

    public Transaction get(String transactionId) {
        return transactions.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("unknown transaction " + transactionId));
    }

    @Transactional
    public Transaction cancel(String transactionId) {
        Transaction txn = get(transactionId);
        txn.cancel();
        carts.findById(txn.getCartId()).ifPresent(c -> {
            c.cancel();
            carts.save(c);
        });
        transactions.save(txn);
        events.publish("checkout-events", DomainEvent.of(EventType.TRANSACTION_CANCELLED, txn.getId(), txn.getId(),
                Map.of("transactionId", txn.getId())));
        return txn;
    }

    private long pointsFor(BigDecimal total) {
        return total.longValue();
    }
}
