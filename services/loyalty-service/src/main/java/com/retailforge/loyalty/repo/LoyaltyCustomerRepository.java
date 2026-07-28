package com.retailforge.loyalty.repo;

import com.retailforge.loyalty.domain.LoyaltyCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyCustomerRepository extends JpaRepository<LoyaltyCustomer, String> {
}
