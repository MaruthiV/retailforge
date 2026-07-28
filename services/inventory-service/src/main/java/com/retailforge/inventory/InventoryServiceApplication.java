package com.retailforge.inventory;

import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.LoggingEventPublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    EventPublisher eventPublisher() {
        return new LoggingEventPublisher();
    }
}
