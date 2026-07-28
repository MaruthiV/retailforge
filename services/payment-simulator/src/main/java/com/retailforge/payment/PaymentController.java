package com.retailforge.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    public enum Status { APPROVED, DECLINED, TIMEOUT }

    public record ChargeRequest(@NotBlank String transactionId, @NotNull BigDecimal amount, @NotBlank String card) {}

    public record ChargeResponse(String transactionId, Status status, String authCode, String reason) {}

    @PostMapping("/charge")
    public ChargeResponse charge(@RequestBody ChargeRequest req) {
        // deterministic scenarios keyed on the card suffix so incidents are reproducible
        if (req.card().endsWith("0002")) {
            return new ChargeResponse(req.transactionId(), Status.DECLINED, null, "insufficient_funds");
        }
        if (req.card().endsWith("0069")) {
            return new ChargeResponse(req.transactionId(), Status.TIMEOUT, null, "gateway_timeout");
        }
        String auth = "AUTH-" + UUID.nameUUIDFromBytes(req.transactionId().getBytes()).toString().substring(0, 8);
        return new ChargeResponse(req.transactionId(), Status.APPROVED, auth, null);
    }
}
