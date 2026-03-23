package com.ecommerce.inventory.domain.event;

import com.ecommerce.common.event.DomainEvent;

public class ReservationConfirmed extends DomainEvent {

    private final String inventoryItemId;
    private final String reservationId;
    private final String orderId;

    public ReservationConfirmed(String inventoryItemId, String reservationId, String orderId) {
        this.inventoryItemId = inventoryItemId;
        this.reservationId = reservationId;
        this.orderId = orderId;
    }

    @Override
    public String getAggregateId() {
        return inventoryItemId;
    }

    public String getReservationId() { return reservationId; }
    public String getOrderId() { return orderId; }
}
