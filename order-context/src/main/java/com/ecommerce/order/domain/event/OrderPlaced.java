package com.ecommerce.order.domain.event;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.model.Money;

public class OrderPlaced extends DomainEvent {

    private final String orderId;
    private final String ordererId;
    private final Money totalAmount;

    public OrderPlaced(String orderId, String ordererId, Money totalAmount) {
        this.orderId = orderId;
        this.ordererId = ordererId;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getAggregateId() { return orderId; }

    public String getOrderId() { return orderId; }
    public String getOrdererId() { return ordererId; }
    public Money getTotalAmount() { return totalAmount; }
}
