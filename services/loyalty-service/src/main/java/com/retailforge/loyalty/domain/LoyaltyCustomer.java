package com.retailforge.loyalty.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loyalty_customer")
public class LoyaltyCustomer {

    @Id
    private String id;
    private String name;
    private long pointsBalance;

    protected LoyaltyCustomer() {}

    public LoyaltyCustomer(String id, String name, long pointsBalance) {
        this.id = id;
        this.name = name;
        this.pointsBalance = pointsBalance;
    }

    public void addPoints(long points) {
        this.pointsBalance += points;
    }

    public void subtractPoints(long points) {
        this.pointsBalance -= points;
    }

    public Tier tier() {
        return Tier.forBalance(pointsBalance);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getPointsBalance() { return pointsBalance; }
}
