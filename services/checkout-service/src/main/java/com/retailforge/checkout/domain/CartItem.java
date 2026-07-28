package com.retailforge.checkout.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_item")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cartId;
    private String productId;
    private String name;
    private BigDecimal unitPrice;
    private int quantity;

    protected CartItem() {}

    public CartItem(String cartId, String productId, String name, BigDecimal unitPrice, int quantity) {
        this.cartId = cartId;
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public String getCartId() { return cartId; }
    public String getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
