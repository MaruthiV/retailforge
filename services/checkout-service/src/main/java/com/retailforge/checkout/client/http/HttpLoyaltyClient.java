package com.retailforge.checkout.client.http;

import com.retailforge.checkout.client.LoyaltyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Profile("distributed")
public class HttpLoyaltyClient implements LoyaltyClient {
    private static final Logger log = LoggerFactory.getLogger(HttpLoyaltyClient.class);
    private final RestClient http;

    public HttpLoyaltyClient(@Value("${clients.loyalty-url:http://localhost:8083}") String baseUrl) {
        this.http = RestClient.create(baseUrl);
    }

    @Override
    public void award(String customerId, String transactionId, long points) {
        try {
            http.post().uri("/api/loyalty/earn")
                    .body(Map.of("customerId", customerId, "transactionId", transactionId, "points", points))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("loyalty award failed for txn={}: {}", transactionId, e.getMessage());
        }
    }
}
