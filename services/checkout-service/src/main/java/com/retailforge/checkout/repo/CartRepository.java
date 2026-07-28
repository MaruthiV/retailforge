package com.retailforge.checkout.repo;

import com.retailforge.checkout.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, String> {
}
