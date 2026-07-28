package com.retailforge.checkout.client.http;

import com.retailforge.checkout.client.PricingClient;
import com.retailforge.checkout.domain.CartItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("distributed")
public class HttpPricingClient implements PricingClient {
    private final RestClient http;

    public HttpPricingClient(@Value("${clients.pricing-url:http://localhost:8081}") String baseUrl) {
        this.http = RestClient.create(baseUrl);
    }

    @Override
    public PricedCart price(String cartId, List<CartItem> items, String coupon) {
        List<Map<String, Object>> lines = items.stream().map(i -> Map.<String, Object>of(
                "productId", i.getProductId(), "unitPrice", i.getUnitPrice(), "quantity", i.getQuantity())).toList();
        Map<String, Object> req = new HashMap<>();
        req.put("items", lines);
        req.put("coupon", coupon);
        Map<?, ?> body = http.post().uri("/api/pricing/calculate").body(req).retrieve().body(Map.class);
        return new PricedCart(new BigDecimal(body.get("subtotal").toString()),
                new BigDecimal(body.get("discount").toString()));
    }
}
