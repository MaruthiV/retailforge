package com.retailforge.checkout.client.http;

import com.retailforge.checkout.client.PaymentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Profile("distributed")
public class HttpPaymentClient implements PaymentClient {
    private final RestClient http;

    public HttpPaymentClient(@Value("${clients.payment-url:http://localhost:8085}") String baseUrl) {
        this.http = RestClient.create(baseUrl);
    }

    @Override
    public ChargeOutcome charge(String transactionId, BigDecimal amount, String card) {
        Map<?, ?> body = http.post().uri("/api/payments/charge")
                .body(Map.of("transactionId", transactionId, "amount", amount, "card", card))
                .retrieve().body(Map.class);
        return new ChargeOutcome(Status.valueOf((String) body.get("status")),
                (String) body.get("authCode"), (String) body.get("reason"));
    }
}
