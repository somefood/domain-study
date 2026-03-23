package com.ecommerce.order.domain.event;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.model.Money;

public class OrderPaid extends DomainEvent {

    private final String orderId;
    private final String paymentId;
    private final Money paidAmount;

    public OrderPaid(String orderId, String paymentId, Money paidAmount) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.paidAmount = paidAmount;
    }

    @Override
    public String getAggregateId() { return orderId; }

    public String getOrderId() { return orderId; }
    public String getPaymentId() { return paymentId; }
    public Money getPaidAmount() { return paidAmount; }
}
