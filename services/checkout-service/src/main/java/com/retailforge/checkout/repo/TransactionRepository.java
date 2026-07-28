package com.retailforge.checkout.repo;

import com.retailforge.checkout.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findFirstByCartId(String cartId);
    List<Transaction> findByCartId(String cartId);
}
