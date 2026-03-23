package com.ecommerce.inventory.domain.event;

import com.ecommerce.common.event.DomainEvent;

public class StockReceived extends DomainEvent {

    private final String inventoryItemId;
    private final String skuId;
    private final int quantity;
    private final String reason;

    public StockReceived(String inventoryItemId, String skuId, int quantity, String reason) {
        this.inventoryItemId = inventoryItemId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.reason = reason;
    }

    @Override
    public String getAggregateId() {
        return inventoryItemId;
    }

    public String getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public String getReason() { return reason; }
}
