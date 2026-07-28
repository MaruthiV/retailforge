package com.retailforge.loyalty.service;

import com.retailforge.common.event.DomainEvent;
import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.EventType;
import com.retailforge.loyalty.domain.LedgerEntry;
import com.retailforge.loyalty.domain.LoyaltyCustomer;
import com.retailforge.loyalty.repo.LedgerRepository;
import com.retailforge.loyalty.repo.LoyaltyCustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class LoyaltyService {
    private static final Logger log = LoggerFactory.getLogger(LoyaltyService.class);

    private final LoyaltyCustomerRepository customers;
    private final LedgerRepository ledger;
    private final EventPublisher events;

    public LoyaltyService(LoyaltyCustomerRepository customers, LedgerRepository ledger, EventPublisher events) {
        this.customers = customers;
        this.ledger = ledger;
        this.events = events;
    }

    public LoyaltyCustomer get(String customerId) {
        return customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("unknown customer " + customerId));
    }

    @Transactional
    public LoyaltyCustomer earn(String customerId, String transactionId, long points) {
        LoyaltyCustomer customer = get(customerId);
        // idempotency: a checkout retry replays the same transactionId, only award once
        if (ledger.existsByReferenceTransactionIdAndType(transactionId, LedgerEntry.Type.EARN)) {
            log.info("skipping duplicate earn for txn={} customer={}", transactionId, customerId);
            return customer;
        }
        ledger.save(new LedgerEntry(customerId, LedgerEntry.Type.EARN, points, transactionId));
        customer.addPoints(points);
        customers.save(customer);
        events.publish("loyalty-events", DomainEvent.keyed(
                "earn-" + transactionId, EventType.LOYALTY_POINTS_EARNED, transactionId, transactionId,
                Map.of("customerId", customerId, "points", points, "balance", customer.getPointsBalance())));
        return customer;
    }

    @Transactional
    public LoyaltyCustomer redeem(String customerId, long points) {
        LoyaltyCustomer customer = get(customerId);
        if (customer.getPointsBalance() < points) {
            throw new IllegalStateException("insufficient points for customer " + customerId);
        }
        ledger.save(new LedgerEntry(customerId, LedgerEntry.Type.REDEEM, points, null));
        customer.subtractPoints(points);
        return customers.save(customer);
    }

    public List<LedgerEntry> history(String customerId) {
        return ledger.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
