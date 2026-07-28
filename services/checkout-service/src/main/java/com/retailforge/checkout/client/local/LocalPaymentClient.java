package com.retailforge.checkout.client.local;

import com.retailforge.checkout.client.PaymentClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Profile("!distributed")
public class LocalPaymentClient implements PaymentClient {

    @Override
    public ChargeOutcome charge(String transactionId, BigDecimal amount, String card) {
        if (card.endsWith("0002")) {
            return new ChargeOutcome(Status.DECLINED, null, "insufficient_funds");
        }
        if (card.endsWith("0069")) {
            return new ChargeOutcome(Status.TIMEOUT, null, "gateway_timeout");
        }
        String auth = "AUTH-" + UUID.nameUUIDFromBytes(transactionId.getBytes()).toString().substring(0, 8);
        return new ChargeOutcome(Status.APPROVED, auth, null);
    }
}
