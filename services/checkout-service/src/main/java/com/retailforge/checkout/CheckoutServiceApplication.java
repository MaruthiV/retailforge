package com.retailforge.checkout;

import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.LoggingEventPublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CheckoutServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutServiceApplication.class, args);
    }

    @Bean
    EventPublisher eventPublisher() {
        return new LoggingEventPublisher();
    }
}
