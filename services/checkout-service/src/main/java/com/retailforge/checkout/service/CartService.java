package com.retailforge.checkout.service;

import com.retailforge.checkout.domain.Cart;
import com.retailforge.checkout.domain.CartItem;
import com.retailforge.checkout.repo.CartItemRepository;
import com.retailforge.checkout.repo.CartRepository;
import com.retailforge.common.event.DomainEvent;
import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.EventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartRepository carts;
    private final CartItemRepository items;
    private final EventPublisher events;

    public CartService(CartRepository carts, CartItemRepository items, EventPublisher events) {
        this.carts = carts;
        this.items = items;
        this.events = events;
    }

    @Transactional
    public Cart create(String storeId, String customerId) {
        Cart cart = carts.save(new Cart(storeId, customerId));
        events.publish("checkout-events", DomainEvent.of(EventType.CART_CREATED, cart.getId(), null,
                Map.of("cartId", cart.getId(), "storeId", storeId)));
        return cart;
    }

    public Cart get(String cartId) {
        return carts.findById(cartId).orElseThrow(() -> new IllegalArgumentException("unknown cart " + cartId));
    }

    public List<CartItem> items(String cartId) {
        return items.findByCartId(cartId);
    }

    @Transactional
    public CartItem addItem(String cartId, String productId, String name, BigDecimal unitPrice, int quantity) {
        Cart cart = get(cartId);
        if (cart.getStatus() != Cart.Status.OPEN) {
            throw new IllegalStateException("cart " + cartId + " is not open");
        }
        CartItem item = items.save(new CartItem(cartId, productId, name, unitPrice, quantity));
        events.publish("checkout-events", DomainEvent.of(EventType.ITEM_ADDED, cartId, null,
                Map.of("cartId", cartId, "productId", productId, "quantity", quantity)));
        return item;
    }

    @Transactional
    public void removeItem(String cartId, Long itemId) {
        CartItem item = items.findByCartIdAndId(cartId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("item not in cart"));
        items.delete(item);
    }
}
