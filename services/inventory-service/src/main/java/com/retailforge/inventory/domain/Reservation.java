package com.retailforge.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "reservation")
public class Reservation {

    public enum Status { RESERVED, RELEASED, COMMITTED }

    @Id
    private String id;
    private String transactionId;
    private String storeId;
    private String productId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private Status status;

    protected Reservation() {}

    public Reservation(String transactionId, String storeId, String productId, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.transactionId = transactionId;
        this.storeId = storeId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = Status.RESERVED;
    }

    public void release() { this.status = Status.RELEASED; }
    public void commit() { this.status = Status.COMMITTED; }

    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getStoreId() { return storeId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }
}
