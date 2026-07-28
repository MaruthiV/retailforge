package com.retailforge.checkout.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "cart")
public class Cart {

    public enum Status { OPEN, CHECKED_OUT, CANCELLED }

    @Id
    private String id;
    private String storeId;
    private String customerId;

    @Enumerated(EnumType.STRING)
    private Status status;

    protected Cart() {}

    public Cart(String storeId, String customerId) {
        this.id = UUID.randomUUID().toString();
        this.storeId = storeId;
        this.customerId = customerId;
        this.status = Status.OPEN;
    }

    public void checkout() { this.status = Status.CHECKED_OUT; }
    public void cancel() { this.status = Status.CANCELLED; }

    public String getId() { return id; }
    public String getStoreId() { return storeId; }
    public String getCustomerId() { return customerId; }
    public Status getStatus() { return status; }
}
