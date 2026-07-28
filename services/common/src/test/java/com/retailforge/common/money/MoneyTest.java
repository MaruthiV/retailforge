package com.retailforge.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void normalizesToTwoDecimals() {
        assertEquals(new BigDecimal("9.99"), Money.of("9.99"));
        assertEquals(new BigDecimal("10.00"), Money.of(10));
    }

    @Test
    void percentOfRoundsHalfUp() {
        assertEquals(new BigDecimal("1.67"), Money.percentOf(Money.of("16.66"), Money.of("10")));
    }

    @Test
    void multiplyByQuantity() {
        assertEquals(new BigDecimal("29.97"), Money.multiply(Money.of("9.99"), 3));
    }
}
