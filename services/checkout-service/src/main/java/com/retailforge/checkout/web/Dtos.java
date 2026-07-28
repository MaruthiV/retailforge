package com.retailforge.checkout.web;

import com.retailforge.checkout.domain.Cart;
import com.retailforge.checkout.domain.CartItem;
import com.retailforge.checkout.domain.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record CreateCartRequest(@NotBlank String storeId, String customerId) {}

    public record AddItemRequest(@NotBlank String productId, @NotBlank String name,
                                 @NotNull BigDecimal unitPrice, @Positive int quantity) {}

    public record CheckoutRequest(@NotBlank String card, String coupon) {}

    public record CartView(String id, String storeId, String customerId, String status, List<ItemView> items) {
        public static CartView of(Cart cart, List<CartItem> items) {
            return new CartView(cart.getId(), cart.getStoreId(), cart.getCustomerId(), cart.getStatus().name(),
                    items.stream().map(ItemView::of).toList());
        }
    }

    public record ItemView(Long id, String productId, String name, BigDecimal unitPrice, int quantity) {
        public static ItemView of(CartItem i) {
            return new ItemView(i.getId(), i.getProductId(), i.getName(), i.getUnitPrice(), i.getQuantity());
        }
    }

    public record TransactionView(String id, String cartId, String customerId, String status,
                                  BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total, String authCode) {
        public static TransactionView of(Transaction t) {
            return new TransactionView(t.getId(), t.getCartId(), t.getCustomerId(), t.getStatus().name(),
                    t.getSubtotal(), t.getDiscount(), t.getTax(), t.getTotal(), t.getAuthCode());
        }
    }
}
