package com.ecommerce.inventory.domain.event;

import com.ecommerce.common.event.DomainEvent;

import java.time.LocalDateTime;

public class StockReserved extends DomainEvent {

    private final String inventoryItemId;
    private final String skuId;
    private final String orderId;
    private final int quantity;
    private final LocalDateTime expiresAt;

    public StockReserved(String inventoryItemId, String skuId, String orderId, int quantity, LocalDateTime expiresAt) {
        this.inventoryItemId = inventoryItemId;
        this.skuId = skuId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    @Override
    public String getAggregateId() {
        return inventoryItemId;
    }

    public String getSkuId() { return skuId; }
    public String getOrderId() { return orderId; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
