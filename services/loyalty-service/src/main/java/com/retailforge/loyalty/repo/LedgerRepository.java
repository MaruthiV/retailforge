package com.retailforge.loyalty.repo;

import com.retailforge.loyalty.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    boolean existsByReferenceTransactionIdAndType(String referenceTransactionId, LedgerEntry.Type type);

    List<LedgerEntry> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
