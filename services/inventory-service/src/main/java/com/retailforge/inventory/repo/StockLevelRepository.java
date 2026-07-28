package com.retailforge.inventory.repo;

import com.retailforge.inventory.domain.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, String> {
    Optional<StockLevel> findByStoreIdAndProductId(String storeId, String productId);
}
