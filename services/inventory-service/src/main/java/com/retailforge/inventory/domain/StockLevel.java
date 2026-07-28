package com.retailforge.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_level")
public class StockLevel {

    @Id
    private String id;
    private String storeId;
    private String productId;
    private int available;
    private int reserved;

    protected StockLevel() {}

    public StockLevel(String storeId, String productId, int available) {
        this.id = storeId + ":" + productId;
        this.storeId = storeId;
        this.productId = productId;
        this.available = available;
        this.reserved = 0;
    }

    public void reserve(int qty) {
        this.available -= qty;
        this.reserved += qty;
    }

    public void release(int qty) {
        this.available += qty;
        this.reserved -= qty;
    }

    public void commit(int qty) {
        this.reserved -= qty;
    }

    public String getId() { return id; }
    public String getStoreId() { return storeId; }
    public String getProductId() { return productId; }
    public int getAvailable() { return available; }
    public int getReserved() { return reserved; }
}
