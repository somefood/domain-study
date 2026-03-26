package com.ecommerce.payment.domain.event;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.model.Money;

public class RefundCompleted extends DomainEvent {

    private final String paymentId;
    private final String refundId;
    private final String orderId;
    private final Money refundedAmount;

    public RefundCompleted(String paymentId, String refundId, String orderId, Money refundedAmount) {
        this.paymentId = paymentId;
        this.refundId = refundId;
        this.orderId = orderId;
        this.refundedAmount = refundedAmount;
    }

    @Override
    public String getAggregateId() { return paymentId; }

    public String getPaymentId() { return paymentId; }
    public String getRefundId() { return refundId; }
    public String getOrderId() { return orderId; }
    public Money getRefundedAmount() { return refundedAmount; }
}
