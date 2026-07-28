package com.retailforge.common.event;

public final class EventType {
    private EventType() {}

    public static final String CART_CREATED = "CartCreated";
    public static final String ITEM_ADDED = "ItemAdded";
    public static final String CHECKOUT_STARTED = "CheckoutStarted";
    public static final String PAYMENT_APPROVED = "PaymentApproved";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String TRANSACTION_COMPLETED = "TransactionCompleted";
    public static final String TRANSACTION_CANCELLED = "TransactionCancelled";
    public static final String LOYALTY_POINTS_EARNED = "LoyaltyPointsEarned";
    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_RELEASED = "InventoryReleased";
}
