package com.retailforge.loyalty.web;

import com.retailforge.loyalty.domain.LedgerEntry;
import com.retailforge.loyalty.domain.LoyaltyCustomer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record EarnRequest(@NotBlank String customerId, @NotBlank String transactionId, @Positive long points) {}

    public record RedeemRequest(@NotBlank String customerId, @Positive long points) {}

    public record CustomerProfile(String id, String name, long pointsBalance, String tier) {
        public static CustomerProfile from(LoyaltyCustomer c) {
            return new CustomerProfile(c.getId(), c.getName(), c.getPointsBalance(), c.tier().name());
        }
    }

    public record HistoryEntry(String type, long points, String referenceTransactionId, Instant createdAt) {
        public static HistoryEntry from(LedgerEntry e) {
            return new HistoryEntry(e.getType().name(), e.getPoints(), e.getReferenceTransactionId(), e.getCreatedAt());
        }
    }

    public record HistoryResponse(String customerId, List<HistoryEntry> entries) {}
}
