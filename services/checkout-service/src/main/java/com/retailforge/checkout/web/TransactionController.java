package com.retailforge.checkout.web;

import com.retailforge.checkout.service.CheckoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final CheckoutService checkout;

    public TransactionController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @GetMapping("/{transactionId}")
    public Dtos.TransactionView get(@PathVariable String transactionId) {
        return Dtos.TransactionView.of(checkout.get(transactionId));
    }

    @PostMapping("/{transactionId}/cancel")
    public Dtos.TransactionView cancel(@PathVariable String transactionId) {
        return Dtos.TransactionView.of(checkout.cancel(transactionId));
    }
}
