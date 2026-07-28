package com.retailforge.inventory.service;

import com.retailforge.common.event.DomainEvent;
import com.retailforge.common.event.EventPublisher;
import com.retailforge.common.event.EventType;
import com.retailforge.inventory.domain.Reservation;
import com.retailforge.inventory.domain.StockLevel;
import com.retailforge.inventory.repo.ReservationRepository;
import com.retailforge.inventory.repo.StockLevelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StockLevelRepository stock;
    private final ReservationRepository reservations;
    private final EventPublisher events;

    public InventoryService(StockLevelRepository stock, ReservationRepository reservations, EventPublisher events) {
        this.stock = stock;
        this.reservations = reservations;
        this.events = events;
    }

    public StockLevel get(String storeId, String productId) {
        return stock.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new IllegalArgumentException("no stock for " + storeId + "/" + productId));
    }

    @Transactional
    public Reservation reserve(String transactionId, String storeId, String productId, int quantity) {
        // idempotency: a replayed reserve event must not decrement stock twice
        List<Reservation> existing = reservations.findByTransactionIdAndStatus(transactionId, Reservation.Status.RESERVED);
        for (Reservation r : existing) {
            if (r.getProductId().equals(productId)) {
                log.info("reservation already exists for txn={} product={}", transactionId, productId);
                return r;
            }
        }
        StockLevel level = get(storeId, productId);
        if (level.getAvailable() < quantity) {
            throw new IllegalStateException("insufficient stock for " + productId);
        }
        level.reserve(quantity);
        stock.save(level);
        Reservation reservation = reservations.save(new Reservation(transactionId, storeId, productId, quantity));
        events.publish("inventory-events", DomainEvent.keyed(
                "reserve-" + transactionId + "-" + productId, EventType.INVENTORY_RESERVED, transactionId, transactionId,
                Map.of("productId", productId, "quantity", quantity)));
        return reservation;
    }

    @Transactional
    public void release(String transactionId) {
        List<Reservation> active = reservations.findByTransactionIdAndStatus(transactionId, Reservation.Status.RESERVED);
        for (Reservation r : active) {
            StockLevel level = get(r.getStoreId(), r.getProductId());
            level.release(r.getQuantity());
            stock.save(level);
            r.release();
            reservations.save(r);
            events.publish("inventory-events", DomainEvent.of(EventType.INVENTORY_RELEASED, transactionId, transactionId,
                    Map.of("productId", r.getProductId(), "quantity", r.getQuantity())));
        }
    }

    @Transactional
    public void commit(String transactionId) {
        List<Reservation> active = reservations.findByTransactionIdAndStatus(transactionId, Reservation.Status.RESERVED);
        for (Reservation r : active) {
            StockLevel level = get(r.getStoreId(), r.getProductId());
            level.commit(r.getQuantity());
            stock.save(level);
            r.commit();
            reservations.save(r);
        }
    }
}
