package com.retailforge.pricing.web;

import com.retailforge.pricing.domain.Promotion;
import com.retailforge.pricing.service.PricingService;
import com.retailforge.pricing.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class PricingController {

    private final PricingService pricing;
    private final PromotionService promotions;

    public PricingController(PricingService pricing, PromotionService promotions) {
        this.pricing = pricing;
        this.promotions = promotions;
    }

    public record CalculateRequest(List<PricingService.Line> items, String coupon) {}

    @PostMapping("/api/pricing/calculate")
    public PricingService.Quote calculate(@RequestBody CalculateRequest req) {
        return pricing.calculate(req.items(), req.coupon());
    }

    @PostMapping("/api/promotions/validate")
    public PromotionService.Validation validate(@RequestBody Map<String, String> body) {
        return promotions.validate(body.get("code"));
    }

    @GetMapping("/api/products/{productId}/price")
    public Map<String, Object> price(@PathVariable String productId) {
        return Map.of("productId", productId, "price", pricing.priceOf(productId));
    }

    @GetMapping("/api/promotions/active")
    public List<Promotion> active() {
        return promotions.active();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> notFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
