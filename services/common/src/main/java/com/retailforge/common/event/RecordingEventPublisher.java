package com.retailforge.common.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// test/in-process publisher so we can assert exactly what was emitted
public class RecordingEventPublisher implements EventPublisher {
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(String topic, DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> all() {
        return List.copyOf(events);
    }

    public List<DomainEvent> byType(String type) {
        return events.stream().filter(e -> e.type().equals(type)).toList();
    }

    public List<DomainEvent> forTransaction(String transactionId) {
        return events.stream().filter(e -> transactionId.equals(e.transactionId())).toList();
    }

    public long countByType(String type) {
        return events.stream().filter(e -> e.type().equals(type)).count();
    }

    public void clear() {
        events.clear();
    }
}
