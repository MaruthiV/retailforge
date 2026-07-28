package com.retailforge.checkout.repo;

import com.retailforge.checkout.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(String cartId);
    Optional<CartItem> findByCartIdAndId(String cartId, Long id);
}
