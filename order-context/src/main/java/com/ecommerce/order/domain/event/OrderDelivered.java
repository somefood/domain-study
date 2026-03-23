package com.ecommerce.order.domain.event;

import com.ecommerce.common.event.DomainEvent;

import java.time.LocalDateTime;

public class OrderDelivered extends DomainEvent {

    private final String orderId;
    private final LocalDateTime deliveredAt;

    public OrderDelivered(String orderId, LocalDateTime deliveredAt) {
        this.orderId = orderId;
        this.deliveredAt = deliveredAt;
    }

    @Override
    public String getAggregateId() { return orderId; }

    public String getOrderId() { return orderId; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
}
