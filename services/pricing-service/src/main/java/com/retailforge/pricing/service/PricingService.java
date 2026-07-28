package com.retailforge.pricing.service;

import com.retailforge.common.money.Money;
import com.retailforge.pricing.domain.Product;
import com.retailforge.pricing.domain.Promotion;
import com.retailforge.pricing.repo.ProductRepository;
import com.retailforge.pricing.repo.PromotionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PricingService {

    public record Line(String productId, BigDecimal unitPrice, int quantity) {}

    public record Quote(BigDecimal subtotal, BigDecimal discount) {}

    private final ProductRepository products;
    private final PromotionRepository promotions;

    public PricingService(ProductRepository products, PromotionRepository promotions) {
        this.products = products;
        this.promotions = promotions;
    }

    @Cacheable("prices")
    public BigDecimal priceOf(String productId) {
        return products.findById(productId).map(Product::getBasePrice)
                .orElseThrow(() -> new IllegalArgumentException("unknown product " + productId));
    }

    public Quote calculate(List<Line> lines, String coupon) {
        BigDecimal subtotal = Money.zero();
        for (Line l : lines) {
            subtotal = Money.add(subtotal, Money.multiply(l.unitPrice(), l.quantity()));
        }
        BigDecimal discount = bestDiscount(lines, subtotal, coupon);
        return new Quote(subtotal, discount);
    }

    // only the single best promotion applies, they do not stack
    private BigDecimal bestDiscount(List<Line> lines, BigDecimal subtotal, String coupon) {
        Instant now = Instant.now();
        BigDecimal best = Money.zero();
        for (Promotion promo : promotions.findByActiveTrue()) {
            if (!promo.isValidAt(now)) continue;
            if (coupon != null && !coupon.equals(promo.getCode())) continue;
            BigDecimal d = discountFor(promo, lines, subtotal);
            if (d.compareTo(best) > 0) best = d;
        }
        return best;
    }

    private BigDecimal discountFor(Promotion promo, List<Line> lines, BigDecimal subtotal) {
        return switch (promo.getType()) {
            case PERCENT -> Money.percentOf(subtotal, promo.getValue());
            case FLAT -> Money.normalize(promo.getValue());
            case BOGO -> bogo(promo, lines);
        };
    }

    private BigDecimal bogo(Promotion promo, List<Line> lines) {
        return lines.stream()
                .filter(l -> l.productId().equals(promo.getProductId()))
                .map(l -> Money.multiply(l.unitPrice(), l.quantity() / 2))
                .reduce(Money.zero(), Money::add);
    }
}
