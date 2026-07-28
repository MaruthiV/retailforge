package com.retailforge.loyalty.domain;

public enum Tier {
    BRONZE, SILVER, GOLD;

    public static Tier forBalance(long balance) {
        if (balance >= 5000) return GOLD;
        if (balance >= 1000) return SILVER;
        return BRONZE;
    }
}
