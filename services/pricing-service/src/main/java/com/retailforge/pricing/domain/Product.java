package com.retailforge.pricing.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    private String id;
    private String name;
    private BigDecimal basePrice;

    protected Product() {}

    public Product(String id, String name, BigDecimal basePrice) {
        this.id = id;
        this.name = name;
        this.basePrice = basePrice;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getBasePrice() { return basePrice; }
}
