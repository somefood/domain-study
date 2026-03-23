package com.ecommerce.order.domain.event;

import com.ecommerce.common.event.DomainEvent;

import java.time.LocalDateTime;

public class OrderCompleted extends DomainEvent {

    private final String orderId;
    private final LocalDateTime completedAt;

    public OrderCompleted(String orderId, LocalDateTime completedAt) {
        this.orderId = orderId;
        this.completedAt = completedAt;
    }

    @Override
    public String getAggregateId() { return orderId; }

    public String getOrderId() { return orderId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
