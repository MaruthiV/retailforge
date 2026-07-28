package com.retailforge.inventory.web;

import com.retailforge.inventory.domain.Reservation;
import com.retailforge.inventory.domain.StockLevel;
import com.retailforge.inventory.service.InventoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    public record ReserveRequest(@NotBlank String transactionId, @NotBlank String storeId,
                                 @NotBlank String productId, @Positive int quantity) {}

    public record TxnRequest(@NotBlank String transactionId) {}

    @GetMapping("/{storeId}/{productId}")
    public StockLevel get(@PathVariable String storeId, @PathVariable String productId) {
        return inventory.get(storeId, productId);
    }

    @PostMapping("/reserve")
    public Reservation reserve(@RequestBody ReserveRequest req) {
        return inventory.reserve(req.transactionId(), req.storeId(), req.productId(), req.quantity());
    }

    @PostMapping("/release")
    public Map<String, String> release(@RequestBody TxnRequest req) {
        inventory.release(req.transactionId());
        return Map.of("status", "released", "transactionId", req.transactionId());
    }

    @PostMapping("/commit")
    public Map<String, String> commit(@RequestBody TxnRequest req) {
        inventory.commit(req.transactionId());
        return Map.of("status", "committed", "transactionId", req.transactionId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> notFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
