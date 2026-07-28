package com.retailforge.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentControllerTest {

    private final PaymentController controller = new PaymentController();

    @Test
    void approvesNormalCard() {
        var resp = controller.charge(new PaymentController.ChargeRequest("t1", new BigDecimal("10.00"), "4111111111111111"));
        assertEquals(PaymentController.Status.APPROVED, resp.status());
        assertNotNull(resp.authCode());
    }

    @Test
    void declinesBadCard() {
        var resp = controller.charge(new PaymentController.ChargeRequest("t2", new BigDecimal("10.00"), "4000000000000002"));
        assertEquals(PaymentController.Status.DECLINED, resp.status());
    }

    @Test
    void timesOutFlaggedCard() {
        var resp = controller.charge(new PaymentController.ChargeRequest("t3", new BigDecimal("10.00"), "4000000000000069"));
        assertEquals(PaymentController.Status.TIMEOUT, resp.status());
    }
}
