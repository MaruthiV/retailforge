package com.retailforge.common.event;

public interface EventPublisher {
    void publish(String topic, DomainEvent event);
}
