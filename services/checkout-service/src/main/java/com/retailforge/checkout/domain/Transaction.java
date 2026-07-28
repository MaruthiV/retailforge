package com.retailforge.checkout.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction")
public class Transaction {

    public enum Status { PENDING, COMPLETED, FAILED, CANCELLED }

    @Id
    private String id;
    private String cartId;
    private String customerId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private String authCode;
    private Instant createdAt;

    protected Transaction() {}

    public Transaction(String cartId, String customerId, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total) {
        this.id = UUID.randomUUID().toString();
        this.cartId = cartId;
        this.customerId = customerId;
        this.status = Status.PENDING;
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.createdAt = Instant.now();
    }

    public void complete(String authCode) {
        this.status = Status.COMPLETED;
        this.authCode = authCode;
    }

    public void fail() { this.status = Status.FAILED; }
    public void cancel() { this.status = Status.CANCELLED; }

    public String getId() { return id; }
    public String getCartId() { return cartId; }
    public String getCustomerId() { return customerId; }
    public Status getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getTotal() { return total; }
    public String getAuthCode() { return authCode; }
    public Instant getCreatedAt() { return createdAt; }
}
