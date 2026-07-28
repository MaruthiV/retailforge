package com.retailforge.inventory;

import com.retailforge.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    InventoryService inventory;

    @Test
    void reserveDecrementsAvailable() {
        int before = inventory.get("store-001", "prod-1").getAvailable();
        inventory.reserve("txn-inv-1", "store-001", "prod-1", 3);
        assertEquals(before - 3, inventory.get("store-001", "prod-1").getAvailable());
    }

    @Test
    void duplicateReserveDoesNotDecrementTwice() {
        int before = inventory.get("store-001", "prod-coffee").getAvailable();
        inventory.reserve("txn-inv-dup", "store-001", "prod-coffee", 4);
        inventory.reserve("txn-inv-dup", "store-001", "prod-coffee", 4);
        assertEquals(before - 4, inventory.get("store-001", "prod-coffee").getAvailable());
    }

    @Test
    void releaseRestoresStock() {
        int before = inventory.get("store-001", "prod-1").getAvailable();
        inventory.reserve("txn-inv-rel", "store-001", "prod-1", 2);
        inventory.release("txn-inv-rel");
        assertEquals(before, inventory.get("store-001", "prod-1").getAvailable());
    }

    @Test
    void cannotOversell() {
        assertThrows(IllegalStateException.class,
                () -> inventory.reserve("txn-inv-over", "store-001", "prod-mug", 999));
    }
}
