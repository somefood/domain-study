package com.ecommerce.payment.domain.model;

import com.ecommerce.common.model.Money;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 환불 Entity.
 *
 * 환불은 "결제 취소"가 아니라 별도의 금융 트랜잭션이다.
 * - 부분 환불 가능: 13만원 결제 중 5만원만 환불
 * - 여러 번 환불 가능: 1차 3만원, 2차 2만원
 * - 환불 총액은 원래 결제 금액을 초과할 수 없음
 *
 * Payment Aggregate 안에 속하는 Entity.
 */
public class Refund {

    private final String refundId;
    private final Money amount;
    private final String reason;
    private final LocalDateTime refundedAt;

    public Refund(Money amount, String reason) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("환불 금액은 양수여야 합니다");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("환불 사유는 필수입니다");
        }
        this.refundId = UUID.randomUUID().toString();
        this.amount = amount;
        this.reason = reason;
        this.refundedAt = LocalDateTime.now();
    }

    public String getRefundId() { return refundId; }
    public Money getAmount() { return amount; }
    public String getReason() { return reason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Refund refund = (Refund) o;
        return refundId.equals(refund.refundId);
    }

    @Override
    public int hashCode() { return Objects.hash(refundId); }
}
