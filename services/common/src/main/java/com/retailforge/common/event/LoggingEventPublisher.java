package com.retailforge.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventPublisher implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger("events");
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void publish(String topic, DomainEvent event) {
        try {
            log.info("event topic={} {}", topic, mapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("could not serialize event {}", event.eventId());
        }
    }
}
