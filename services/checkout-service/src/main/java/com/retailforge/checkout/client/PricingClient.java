package com.retailforge.checkout.client;

import com.retailforge.checkout.domain.CartItem;

import java.math.BigDecimal;
import java.util.List;

public interface PricingClient {

    record PricedCart(BigDecimal subtotal, BigDecimal discount) {}

    PricedCart price(String cartId, List<CartItem> items, String coupon);
}
