package com.retailforge.checkout.client.local;

import com.retailforge.checkout.client.PricingClient;
import com.retailforge.checkout.domain.CartItem;
import com.retailforge.common.money.Money;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("!distributed")
public class LocalPricingClient implements PricingClient {

    @Override
    public PricedCart price(String cartId, List<CartItem> items, String coupon) {
        BigDecimal subtotal = Money.zero();
        for (CartItem item : items) {
            subtotal = Money.add(subtotal, Money.multiply(item.getUnitPrice(), item.getQuantity()));
        }
        BigDecimal discount = discountFor(subtotal, coupon);
        return new PricedCart(subtotal, discount);
    }

    private BigDecimal discountFor(BigDecimal subtotal, String coupon) {
        if (coupon == null) return Money.zero();
        return switch (coupon) {
            case "SAVE10" -> Money.percentOf(subtotal, Money.of(10));
            case "SAVE5" -> Money.of(5);
            default -> Money.zero();
        };
    }
}
