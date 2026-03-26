package com.ecommerce.payment.domain.model;

import com.ecommerce.common.model.AggregateRoot;
import com.ecommerce.common.model.Money;
import com.ecommerce.payment.domain.event.PaymentCompleted;
import com.ecommerce.payment.domain.event.PaymentFailed;
import com.ecommerce.payment.domain.event.RefundCompleted;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 결제 Aggregate Root.
 *
 * 결제 상태 머신:
 *   INITIATED → AUTHORIZED → CAPTURED (정상)
 *   INITIATED → FAILED (결제 실패)
 *   AUTHORIZED → CANCELLED (매입 전 취소)
 *   CAPTURED → PARTIALLY_REFUNDED / FULLY_REFUNDED (환불)
 *
 * 불변식:
 * 1. 환불 총액은 결제 금액을 초과할 수 없다
 * 2. 상태 전이 규칙을 반드시 따름
 * 3. 멱등성 키로 중복 결제 방지
 */
public class Payment extends AggregateRoot {

    private final PaymentId paymentId;
    private final String orderId;
    private final Money amount;
    private final PaymentMethod method;
    private final String idempotencyKey;
    private PaymentStatus status;
    private String pgTransactionId;
    private final List<Refund> refunds;
    private final LocalDateTime initiatedAt;
    private LocalDateTime capturedAt;

    /**
     * 결제 시작.
     *
     * @param idempotencyKey 멱등성 키 — 같은 키로 두 번 결제하면 중복 방지
     */
    public static Payment initiate(String orderId, Money amount, PaymentMethod method, String idempotencyKey) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("주문 ID는 필수입니다");
        }
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("결제 금액은 양수여야 합니다");
        }
        if (method == null) {
            throw new IllegalArgumentException("결제 수단은 필수입니다");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키는 필수입니다");
        }
        return new Payment(orderId, amount, method, idempotencyKey);
    }

    private Payment(String orderId, Money amount, PaymentMethod method, String idempotencyKey) {
        this.paymentId = PaymentId.generate();
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.INITIATED;
        this.refunds = new ArrayList<>();
        this.initiatedAt = LocalDateTime.now();
    }

    /**
     * PG 인증+매입 성공 처리.
     * 학습 편의상 authorize와 capture를 합쳐서 처리.
     */
    public void complete(String pgTransactionId) {
        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("INITIATED 상태에서만 완료할 수 있습니다. 현재: " + status);
        }
        this.status = PaymentStatus.CAPTURED;
        this.pgTransactionId = pgTransactionId;
        this.capturedAt = LocalDateTime.now();

        registerEvent(new PaymentCompleted(
                paymentId.getId(), orderId, amount, capturedAt
        ));
    }

    /**
     * 결제 실패 처리.
     */
    public void fail(String reason) {
        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("INITIATED 상태에서만 실패 처리할 수 있습니다. 현재: " + status);
        }
        this.status = PaymentStatus.FAILED;

        registerEvent(new PaymentFailed(
                paymentId.getId(), orderId, reason
        ));
    }

    /**
     * 환불 요청.
     *
     * 불변식: 환불 총액은 결제 금액을 초과할 수 없다.
     * CAPTURED 상태에서만 환불 가능 (매입이 완료된 후에야 돌려줄 돈이 있으니까).
     */
    public Refund refund(Money refundAmount, String reason) {
        if (status != PaymentStatus.CAPTURED && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("CAPTURED 또는 PARTIALLY_REFUNDED 상태에서만 환불할 수 있습니다. 현재: " + status);
        }

        Money totalRefunded = getTotalRefundedAmount().add(refundAmount);
        if (!amount.isGreaterThanOrEqual(totalRefunded)) {
            throw new IllegalArgumentException(
                    String.format("환불 총액이 결제 금액을 초과합니다. 결제: %s, 환불 요청 후 총액: %s",
                            amount, totalRefunded)
            );
        }

        Refund refund = new Refund(refundAmount, reason);
        refunds.add(refund);

        // 전체 환불인지 부분 환불인지 판단
        if (totalRefunded.equals(amount)) {
            this.status = PaymentStatus.FULLY_REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }

        registerEvent(new RefundCompleted(
                paymentId.getId(), refund.getRefundId(), orderId, refundAmount
        ));

        return refund;
    }

    /**
     * 지금까지 환불된 총액.
     */
    public Money getTotalRefundedAmount() {
        return refunds.stream()
                .map(Refund::getAmount)
                .reduce(Money.krw(0), Money::add);
    }

    // Getters
    public PaymentId getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public Money getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public PaymentStatus getStatus() { return status; }
    public String getPgTransactionId() { return pgTransactionId; }
    public List<Refund> getRefunds() { return Collections.unmodifiableList(refunds); }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
}
