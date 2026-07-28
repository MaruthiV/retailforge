package com.retailforge.pricing.repo;

import com.retailforge.pricing.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, String> {
    List<Promotion> findByActiveTrue();
}
