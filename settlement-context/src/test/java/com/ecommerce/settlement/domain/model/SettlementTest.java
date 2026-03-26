package com.ecommerce.settlement.domain.model;

import com.ecommerce.common.model.Money;
import com.ecommerce.settlement.domain.event.SettlementApproved;
import com.ecommerce.settlement.domain.event.SettlementCalculated;
import com.ecommerce.settlement.domain.event.SettlementPaid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class SettlementTest {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.10"); // 10%
    private static final SettlementPeriod THIS_WEEK =
            new SettlementPeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 7));

    private Settlement createSettlement() {
        return Settlement.create("seller-1", THIS_WEEK);
    }

    // ── 정산 생성 및 항목 추가 ──

    @Nested
    @DisplayName("정산 항목 관리")
    class EntryManagement {

        @Test
        @DisplayName("정산을 생성하면 PENDING 상태이다")
        void create_pendingStatus() {
            Settlement settlement = createSettlement();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
            assertThat(settlement.getSellerId()).isEqualTo("seller-1");
        }

        @Test
        @DisplayName("결제 항목을 추가하고 금액이 올바르게 계산된다")
        void addEntry_calculatesCorrectly() {
            Settlement settlement = createSettlement();

            // 5만원 결제, 수수료 10%
            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);

            assertThat(settlement.getEntries()).hasSize(1);
            SettlementEntry entry = settlement.getEntries().get(0);
            assertThat(entry.getOrderAmount()).isEqualTo(Money.krw(50000));
            assertThat(entry.getCommissionAmount()).isEqualTo(Money.krw(5000));  // 50000 × 10%
            assertThat(entry.getNetAmount()).isEqualTo(Money.krw(45000));        // 50000 - 5000
        }

        @Test
        @DisplayName("여러 결제 항목의 합산이 올바르다")
        void multipleEntries_totalsCorrect() {
            Settlement settlement = createSettlement();

            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);  // 순: 45,000
            settlement.addEntry("pay-2", Money.krw(80000), COMMISSION_RATE);  // 순: 72,000

            assertThat(settlement.getTotalGrossAmount()).isEqualTo(Money.krw(130000));
            assertThat(settlement.getTotalCommission()).isEqualTo(Money.krw(13000));
            assertThat(settlement.getTotalNetAmount()).isEqualTo(Money.krw(117000));
        }
    }

    // ── 정산 계산 확정 ──

    @Nested
    @DisplayName("정산 계산")
    class Calculation {

        @Test
        @DisplayName("항목 추가 후 계산 확정하면 CALCULATED 상태가 된다")
        void calculate_changesStatus() {
            Settlement settlement = createSettlement();
            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);

            settlement.calculate();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CALCULATED);
        }

        @Test
        @DisplayName("계산 확정 시 SettlementCalculated 이벤트가 발행된다")
        void calculate_publishesEvent() {
            Settlement settlement = createSettlement();
            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);

            settlement.calculate();

            assertThat(settlement.getEvents()).hasSize(1);
            SettlementCalculated event = (SettlementCalculated) settlement.getEvents().get(0);
            assertThat(event.getSellerId()).isEqualTo("seller-1");
            assertThat(event.getNetAmount()).isEqualTo(Money.krw(45000));
        }

        @Test
        @DisplayName("항목이 없으면 계산할 수 없다")
        void calculate_noEntries_throwsException() {
            Settlement settlement = createSettlement();

            assertThatThrownBy(settlement::calculate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("항목이 없습니다");
        }
    }

    // ── 환불 차감 ──

    @Nested
    @DisplayName("환불 차감")
    class RefundDeduction {

        @Test
        @DisplayName("환불 발생 시 해당 항목에서 차감되고 금액이 재계산된다")
        void applyRefund_recalculates() {
            Settlement settlement = createSettlement();
            settlement.addEntry("pay-1", Money.krw(100000), COMMISSION_RATE);

            // 3만원 환불
            settlement.applyRefund("pay-1", Money.krw(30000));

            SettlementEntry entry = settlement.getEntries().get(0);
            assertThat(entry.getRefundDeductions()).isEqualTo(Money.krw(30000));
            // 순 매출: 100,000 - 30,000 = 70,000
            // 수수료: 70,000 × 10% = 7,000
            // 셀러 수령액: 70,000 - 7,000 = 63,000
            assertThat(entry.getCommissionAmount()).isEqualTo(Money.krw(7000));
            assertThat(entry.getNetAmount()).isEqualTo(Money.krw(63000));
        }

        @Test
        @DisplayName("여러 결제 + 환불이 섞인 정산 계산")
        void complexSettlement() {
            Settlement settlement = createSettlement();

            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);   // 순: 45,000
            settlement.addEntry("pay-2", Money.krw(80000), COMMISSION_RATE);   // 순: 72,000

            // pay-2에서 3만원 환불
            settlement.applyRefund("pay-2", Money.krw(30000));
            // pay-2: 순 매출 50,000, 수수료 5,000, 셀러 45,000

            // 총: 45,000 + 45,000 = 90,000
            assertThat(settlement.getTotalNetAmount()).isEqualTo(Money.krw(90000));
            assertThat(settlement.getTotalRefundDeductions()).isEqualTo(Money.krw(30000));
        }

        @Test
        @DisplayName("이미 지급 완료된 정산에는 환불을 적용할 수 없다")
        void applyRefund_afterPaid_throwsException() {
            Settlement settlement = createSettlement();
            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);
            settlement.calculate();
            settlement.approve();
            settlement.markPaid();

            assertThatThrownBy(() -> settlement.applyRefund("pay-1", Money.krw(10000)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("지급 완료");
        }
    }

    // ── 상태 전이: 전체 플로우 ──

    @Nested
    @DisplayName("전체 플로우")
    class FullFlow {

        @Test
        @DisplayName("PENDING → CALCULATED → APPROVED → PAID")
        void fullFlow() {
            Settlement settlement = createSettlement();
            settlement.addEntry("pay-1", Money.krw(100000), COMMISSION_RATE);

            // 계산
            settlement.calculate();
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CALCULATED);

            // 승인
            settlement.approve();
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.APPROVED);

            // 지급
            settlement.markPaid();
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PAID);

            // 이벤트 3개: Calculated, Approved, Paid
            assertThat(settlement.getEvents()).hasSize(3);
            assertThat(settlement.getEvents().get(0)).isInstanceOf(SettlementCalculated.class);
            assertThat(settlement.getEvents().get(1)).isInstanceOf(SettlementApproved.class);
            assertThat(settlement.getEvents().get(2)).isInstanceOf(SettlementPaid.class);
        }

        @Test
        @DisplayName("PENDING에서 바로 승인할 수 없다")
        void approve_fromPending_throwsException() {
            Settlement settlement = createSettlement();
            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);

            assertThatThrownBy(settlement::approve)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("계산 확정 후에는 항목을 추가할 수 없다")
        void addEntry_afterCalculated_throwsException() {
            Settlement settlement = createSettlement();
            settlement.addEntry("pay-1", Money.krw(50000), COMMISSION_RATE);
            settlement.calculate();

            assertThatThrownBy(() -> settlement.addEntry("pay-2", Money.krw(30000), COMMISSION_RATE))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
