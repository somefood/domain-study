package com.ecommerce.payment.domain.event;

import com.ecommerce.common.event.DomainEvent;

/**
 * 결제 실패 이벤트.
 * Saga가 이 이벤트를 받으면 재고 예약 취소 + 주문 취소를 실행한다.
 */
public class PaymentFailed extends DomainEvent {

    private final String paymentId;
    private final String orderId;
    private final String reason;

    public PaymentFailed(String paymentId, String orderId, String reason) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.reason = reason;
    }

    @Override
    public String getAggregateId() { return paymentId; }

    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public String getReason() { return reason; }
}
