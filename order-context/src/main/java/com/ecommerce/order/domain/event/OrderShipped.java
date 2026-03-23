package com.ecommerce.order.domain.event;

import com.ecommerce.common.event.DomainEvent;

public class OrderShipped extends DomainEvent {

    private final String orderId;
    private final String trackingNumber;

    public OrderShipped(String orderId, String trackingNumber) {
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
    }

    @Override
    public String getAggregateId() { return orderId; }

    public String getOrderId() { return orderId; }
    public String getTrackingNumber() { return trackingNumber; }
}
