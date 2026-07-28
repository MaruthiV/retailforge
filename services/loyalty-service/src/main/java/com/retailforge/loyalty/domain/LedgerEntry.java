package com.retailforge.loyalty.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "loyalty_ledger")
public class LedgerEntry {

    public enum Type { EARN, REDEEM, REVERSAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private Type type;

    private long points;

    // the checkout transaction this entry came from, used as the idempotency key
    private String referenceTransactionId;

    private Instant createdAt;

    protected LedgerEntry() {}

    public LedgerEntry(String customerId, Type type, long points, String referenceTransactionId) {
        this.customerId = customerId;
        this.type = type;
        this.points = points;
        this.referenceTransactionId = referenceTransactionId;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public Type getType() { return type; }
    public long getPoints() { return points; }
    public String getReferenceTransactionId() { return referenceTransactionId; }
    public Instant getCreatedAt() { return createdAt; }
}
