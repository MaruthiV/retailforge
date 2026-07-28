package com.retailforge.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

// money is always 2dp, half-up. rounding bugs are a whole incident category so keep this strict
public final class Money {
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {}

    public static BigDecimal of(String amount) {
        return normalize(new BigDecimal(amount));
    }

    public static BigDecimal of(double amount) {
        return normalize(BigDecimal.valueOf(amount));
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return normalize(base.multiply(percent).divide(BigDecimal.valueOf(100)));
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return normalize(a.add(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return normalize(a.subtract(b));
    }

    public static BigDecimal multiply(BigDecimal a, int qty) {
        return normalize(a.multiply(BigDecimal.valueOf(qty)));
    }

    public static BigDecimal zero() {
        return normalize(BigDecimal.ZERO);
    }
}
