package com.ecommerce.order.domain.event;

import com.ecommerce.common.event.DomainEvent;

public class OrderCancelled extends DomainEvent {

    private final String orderId;
    private final String reason;

    public OrderCancelled(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    @Override
    public String getAggregateId() { return orderId; }

    public String getOrderId() { return orderId; }
    public String getReason() { return reason; }
}
