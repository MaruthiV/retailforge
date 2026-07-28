package com.retailforge.checkout.web;

import com.retailforge.checkout.domain.Cart;
import com.retailforge.checkout.domain.Transaction;
import com.retailforge.checkout.service.CartService;
import com.retailforge.checkout.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    public CartController(CartService cartService, CheckoutService checkoutService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
    }

    @PostMapping
    public Dtos.CartView create(@Valid @RequestBody Dtos.CreateCartRequest req) {
        Cart cart = cartService.create(req.storeId(), req.customerId());
        return Dtos.CartView.of(cart, cartService.items(cart.getId()));
    }

    @GetMapping("/{cartId}")
    public Dtos.CartView get(@PathVariable String cartId) {
        return Dtos.CartView.of(cartService.get(cartId), cartService.items(cartId));
    }

    @PostMapping("/{cartId}/items")
    public Dtos.CartView addItem(@PathVariable String cartId, @Valid @RequestBody Dtos.AddItemRequest req) {
        cartService.addItem(cartId, req.productId(), req.name(), req.unitPrice(), req.quantity());
        return Dtos.CartView.of(cartService.get(cartId), cartService.items(cartId));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public Dtos.CartView removeItem(@PathVariable String cartId, @PathVariable Long itemId) {
        cartService.removeItem(cartId, itemId);
        return Dtos.CartView.of(cartService.get(cartId), cartService.items(cartId));
    }

    @PostMapping("/{cartId}/checkout")
    public Dtos.TransactionView checkout(@PathVariable String cartId, @Valid @RequestBody Dtos.CheckoutRequest req) {
        Transaction txn = checkoutService.checkout(cartId, req.card(), req.coupon());
        return Dtos.TransactionView.of(txn);
    }
}
