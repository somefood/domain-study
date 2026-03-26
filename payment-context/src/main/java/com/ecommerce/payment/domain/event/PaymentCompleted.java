package com.ecommerce.payment.domain.event;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.model.Money;

import java.time.LocalDateTime;

/**
 * 결제 완료(매입 완료) 이벤트.
 * 이 이벤트를 주문 컨텍스트가 구독하여 주문을 PAID로 전이시킨다.
 */
public class PaymentCompleted extends DomainEvent {

    private final String paymentId;
    private final String orderId;
    private final Money amount;
    private final LocalDateTime capturedAt;

    public PaymentCompleted(String paymentId, String orderId, Money amount, LocalDateTime capturedAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.capturedAt = capturedAt;
    }

    @Override
    public String getAggregateId() { return paymentId; }

    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public Money getAmount() { return amount; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
}
