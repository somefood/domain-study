package com.ecommerce.settlement.domain.model;

import com.ecommerce.common.model.Money;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * 정산 항목 Entity.
 *
 * 하나의 결제(Payment)에 대응하는 정산 내역.
 * Settlement Aggregate 안에 속한다.
 *
 * 계산 구조:
 *   주문 금액 - 환불 차감 = 순 매출
 *   순 매출 × 수수료율 = 수수료
 *   순 매출 - 수수료 = 셀러 수령액(netAmount)
 */
public class SettlementEntry {

    private final String entryId;
    private final String paymentId;
    private final Money orderAmount;
    private Money refundDeductions;
    private final BigDecimal commissionRate;
    private Money commissionAmount;
    private Money netAmount;

    public SettlementEntry(String paymentId, Money orderAmount, BigDecimal commissionRate) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("결제 ID는 필수입니다");
        }
        if (orderAmount == null || !orderAmount.isPositive()) {
            throw new IllegalArgumentException("주문 금액은 양수여야 합니다");
        }
        this.entryId = UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.orderAmount = orderAmount;
        this.refundDeductions = Money.krw(0);
        this.commissionRate = commissionRate;
        calculate();
    }

    /**
     * 환불 차감 적용.
     * 정산 계산 후 지급 전에 환불이 발생하면 차감.
     */
    public void applyRefundDeduction(Money refundAmount) {
        this.refundDeductions = this.refundDeductions.add(refundAmount);
        calculate();
    }

    /**
     * 순 매출, 수수료, 셀러 수령액을 재계산.
     */
    private void calculate() {
        Money netSales = orderAmount.subtract(refundDeductions);
        // 수수료 = 순 매출 × 수수료율
        BigDecimal commissionValue = netSales.getAmount().multiply(commissionRate);
        this.commissionAmount = new Money(commissionValue, orderAmount.getCurrency());
        // 셀러 수령액 = 순 매출 - 수수료
        this.netAmount = netSales.subtract(commissionAmount);
    }

    public String getEntryId() { return entryId; }
    public String getPaymentId() { return paymentId; }
    public Money getOrderAmount() { return orderAmount; }
    public Money getRefundDeductions() { return refundDeductions; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public Money getCommissionAmount() { return commissionAmount; }
    public Money getNetAmount() { return netAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettlementEntry that = (SettlementEntry) o;
        return entryId.equals(that.entryId);
    }

    @Override
    public int hashCode() { return Objects.hash(entryId); }
}
