package com.retailforge.pricing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion")
public class Promotion {

    public enum Type { PERCENT, FLAT, BOGO }

    @Id
    private String code;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "amount")
    private BigDecimal value;
    private String productId;
    private Instant startsAt;
    private Instant endsAt;
    private boolean active;

    protected Promotion() {}

    public Promotion(String code, Type type, BigDecimal value, String productId, Instant startsAt, Instant endsAt, boolean active) {
        this.code = code;
        this.type = type;
        this.value = value;
        this.productId = productId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = active;
    }

    public boolean isValidAt(Instant now) {
        return active && !now.isBefore(startsAt) && !now.isAfter(endsAt);
    }

    public String getCode() { return code; }
    public Type getType() { return type; }
    public BigDecimal getValue() { return value; }
    public String getProductId() { return productId; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isActive() { return active; }
}
