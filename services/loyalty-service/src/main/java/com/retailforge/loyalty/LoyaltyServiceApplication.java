package com.retailforge.loyalty;

import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.LoggingEventPublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LoyaltyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoyaltyServiceApplication.class, args);
    }

    @Bean
    EventPublisher eventPublisher() {
        return new LoggingEventPublisher();
    }
}
