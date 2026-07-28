package com.retailforge.pricing.service;

import com.retailforge.pricing.domain.Promotion;
import com.retailforge.pricing.repo.PromotionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PromotionService {

    public record Validation(String code, boolean valid, String reason) {}

    private final PromotionRepository promotions;

    public PromotionService(PromotionRepository promotions) {
        this.promotions = promotions;
    }

    public Validation validate(String code) {
        Promotion promo = promotions.findById(code).orElse(null);
        if (promo == null) return new Validation(code, false, "unknown_code");
        if (!promo.isActive()) return new Validation(code, false, "inactive");
        if (!promo.isValidAt(Instant.now())) return new Validation(code, false, "expired");
        return new Validation(code, true, null);
    }

    public List<Promotion> active() {
        return promotions.findByActiveTrue().stream()
                .filter(p -> p.isValidAt(Instant.now()))
                .toList();
    }
}
