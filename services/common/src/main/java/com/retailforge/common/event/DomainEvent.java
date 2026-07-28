package com.retailforge.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        String eventId,
        String type,
        String traceId,
        String transactionId,
        Instant occurredAt,
        Map<String, Object> payload) {

    public static DomainEvent of(String type, String traceId, String transactionId, Map<String, Object> payload) {
        return new DomainEvent(UUID.randomUUID().toString(), type, traceId, transactionId, Instant.now(), payload);
    }

    // same business key, deterministic id — used when a producer wants dedupe across retries
    public static DomainEvent keyed(String eventId, String type, String traceId, String transactionId, Map<String, Object> payload) {
        return new DomainEvent(eventId, type, traceId, transactionId, Instant.now(), payload);
    }
}
