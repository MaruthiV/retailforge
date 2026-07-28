package com.retailforge.loyalty.web;

import com.retailforge.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyalty;

    public LoyaltyController(LoyaltyService loyalty) {
        this.loyalty = loyalty;
    }

    @GetMapping("/customers/{customerId}")
    public Dtos.CustomerProfile get(@PathVariable String customerId) {
        return Dtos.CustomerProfile.from(loyalty.get(customerId));
    }

    @PostMapping("/earn")
    public Dtos.CustomerProfile earn(@Valid @RequestBody Dtos.EarnRequest req) {
        return Dtos.CustomerProfile.from(loyalty.earn(req.customerId(), req.transactionId(), req.points()));
    }

    @PostMapping("/redeem")
    public Dtos.CustomerProfile redeem(@Valid @RequestBody Dtos.RedeemRequest req) {
        return Dtos.CustomerProfile.from(loyalty.redeem(req.customerId(), req.points()));
    }

    @GetMapping("/customers/{customerId}/history")
    public Dtos.HistoryResponse history(@PathVariable String customerId) {
        return new Dtos.HistoryResponse(customerId,
                loyalty.history(customerId).stream().map(Dtos.HistoryEntry::from).toList());
    }
}
