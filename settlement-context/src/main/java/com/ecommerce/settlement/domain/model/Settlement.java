package com.ecommerce.settlement.domain.model;

import com.ecommerce.common.model.AggregateRoot;
import com.ecommerce.common.model.Money;
import com.ecommerce.settlement.domain.event.SettlementApproved;
import com.ecommerce.settlement.domain.event.SettlementCalculated;
import com.ecommerce.settlement.domain.event.SettlementPaid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 정산 Aggregate Root.
 *
 * 특정 셀러의 특정 기간에 대한 정산.
 * 해당 기간에 완료된 결제들을 모아서 수수료를 차감하고 셀러 수령액을 계산.
 *
 * 상태 전이:
 *   PENDING → CALCULATED: 정산 항목 추가 후 계산 확정
 *   CALCULATED → APPROVED: 운영팀 승인
 *   APPROVED → PAID: 셀러 계좌로 지급 완료
 *
 * 불변식:
 * - 이미 PAID된 정산에 항목을 추가하거나 수정할 수 없다
 * - 환불 차감 후에도 셀러 수령액이 음수가 되면 안 된다 (다음 정산으로 이월)
 */
public class Settlement extends AggregateRoot {

    private final SettlementId settlementId;
    private final String sellerId;
    private final SettlementPeriod period;
    private final List<SettlementEntry> entries;
    private SettlementStatus status;

    public static Settlement create(String sellerId, SettlementPeriod period) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("셀러 ID는 필수입니다");
        }
        if (period == null) {
            throw new IllegalArgumentException("정산 기간은 필수입니다");
        }
        return new Settlement(sellerId, period);
    }

    private Settlement(String sellerId, SettlementPeriod period) {
        this.settlementId = SettlementId.generate();
        this.sellerId = sellerId;
        this.period = period;
        this.entries = new ArrayList<>();
        this.status = SettlementStatus.PENDING;
    }

    /**
     * 정산 항목 추가.
     * 해당 기간에 완료된 결제 1건에 대한 정산 내역.
     */
    public void addEntry(String paymentId, Money orderAmount, BigDecimal commissionRate) {
        if (status != SettlementStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 항목을 추가할 수 있습니다. 현재: " + status);
        }
        entries.add(new SettlementEntry(paymentId, orderAmount, commissionRate));
    }

    /**
     * 정산 계산 확정.
     * 모든 항목 추가가 끝난 후 호출.
     */
    public void calculate() {
        if (status != SettlementStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 계산할 수 있습니다. 현재: " + status);
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException("정산 항목이 없습니다");
        }
        this.status = SettlementStatus.CALCULATED;
        registerEvent(new SettlementCalculated(
                settlementId.getId(), sellerId, getTotalNetAmount()
        ));
    }

    /**
     * 환불 차감 적용.
     * 정산 계산 후 지급 전에 환불이 발생하면 해당 항목에서 차감.
     */
    public void applyRefund(String paymentId, Money refundAmount) {
        if (status == SettlementStatus.PAID) {
            throw new IllegalStateException("이미 지급 완료된 정산에는 환불을 적용할 수 없습니다");
        }
        SettlementEntry entry = entries.stream()
                .filter(e -> e.getPaymentId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 결제의 정산 항목을 찾을 수 없습니다: " + paymentId));

        entry.applyRefundDeduction(refundAmount);
    }

    /**
     * 운영팀 승인.
     */
    public void approve() {
        if (status != SettlementStatus.CALCULATED) {
            throw new IllegalStateException("CALCULATED 상태에서만 승인할 수 있습니다. 현재: " + status);
        }
        this.status = SettlementStatus.APPROVED;
        registerEvent(new SettlementApproved(settlementId.getId()));
    }

    /**
     * 셀러에게 지급 완료.
     */
    public void markPaid() {
        if (status != SettlementStatus.APPROVED) {
            throw new IllegalStateException("APPROVED 상태에서만 지급 처리할 수 있습니다. 현재: " + status);
        }
        this.status = SettlementStatus.PAID;
        registerEvent(new SettlementPaid(settlementId.getId(), LocalDateTime.now()));
    }

    // ── 합산 조회 (파생값) ──

    public Money getTotalGrossAmount() {
        return entries.stream()
                .map(SettlementEntry::getOrderAmount)
                .reduce(Money.krw(0), Money::add);
    }

    public Money getTotalRefundDeductions() {
        return entries.stream()
                .map(SettlementEntry::getRefundDeductions)
                .reduce(Money.krw(0), Money::add);
    }

    public Money getTotalCommission() {
        return entries.stream()
                .map(SettlementEntry::getCommissionAmount)
                .reduce(Money.krw(0), Money::add);
    }

    public Money getTotalNetAmount() {
        return entries.stream()
                .map(SettlementEntry::getNetAmount)
                .reduce(Money.krw(0), Money::add);
    }

    // Getters
    public SettlementId getSettlementId() { return settlementId; }
    public String getSellerId() { return sellerId; }
    public SettlementPeriod getPeriod() { return period; }
    public List<SettlementEntry> getEntries() { return Collections.unmodifiableList(entries); }
    public SettlementStatus getStatus() { return status; }
}
