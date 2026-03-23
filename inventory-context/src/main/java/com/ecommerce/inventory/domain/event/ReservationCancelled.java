package com.ecommerce.inventory.domain.event;

import com.ecommerce.common.event.DomainEvent;

public class ReservationCancelled extends DomainEvent {

    private final String inventoryItemId;
    private final String reservationId;
    private final String orderId;
    private final String reason;

    public ReservationCancelled(String inventoryItemId, String reservationId, String orderId, String reason) {
        this.inventoryItemId = inventoryItemId;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.reason = reason;
    }

    @Override
    public String getAggregateId() {
        return inventoryItemId;
    }

    public String getReservationId() { return reservationId; }
    public String getOrderId() { return orderId; }
    public String getReason() { return reason; }
}
