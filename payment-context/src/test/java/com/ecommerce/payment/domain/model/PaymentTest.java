package com.ecommerce.payment.domain.model;

import com.ecommerce.common.model.Money;
import com.ecommerce.payment.domain.event.PaymentCompleted;
import com.ecommerce.payment.domain.event.PaymentFailed;
import com.ecommerce.payment.domain.event.RefundCompleted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    private Payment createPayment() {
        return Payment.initiate("order-1", Money.krw(100000), PaymentMethod.CARD, "idem-key-1");
    }

    private Payment createCompletedPayment() {
        Payment payment = createPayment();
        payment.complete("pg-txn-001");
        payment.clearEvents();
        return payment;
    }

    // ── 결제 생성 ──

    @Nested
    @DisplayName("결제 생성")
    class Initiation {

        @Test
        @DisplayName("결제를 시작하면 INITIATED 상태이다")
        void initiate_createsWithInitiatedStatus() {
            Payment payment = createPayment();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
            assertThat(payment.getOrderId()).isEqualTo("order-1");
            assertThat(payment.getAmount()).isEqualTo(Money.krw(100000));
            assertThat(payment.getIdempotencyKey()).isEqualTo("idem-key-1");
        }

        @Test
        @DisplayName("금액이 0이면 생성 불가")
        void initiate_zeroAmount_throwsException() {
            assertThatThrownBy(() ->
                    Payment.initiate("order-1", Money.krw(0), PaymentMethod.CARD, "key"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── 결제 완료 ──

    @Nested
    @DisplayName("결제 완료")
    class Completion {

        @Test
        @DisplayName("결제 완료 시 CAPTURED 상태가 된다")
        void complete_changeStatusToCaptured() {
            Payment payment = createPayment();

            payment.complete("pg-txn-001");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(payment.getPgTransactionId()).isEqualTo("pg-txn-001");
            assertThat(payment.getCapturedAt()).isNotNull();
        }

        @Test
        @DisplayName("완료 시 PaymentCompleted 이벤트가 발행된다")
        void complete_publishesEvent() {
            Payment payment = createPayment();

            payment.complete("pg-txn-001");

            assertThat(payment.getEvents()).hasSize(1);
            assertThat(payment.getEvents().get(0)).isInstanceOf(PaymentCompleted.class);
        }

        @Test
        @DisplayName("이미 완료된 결제를 다시 완료할 수 없다")
        void complete_alreadyCaptured_throwsException() {
            Payment payment = createCompletedPayment();

            assertThatThrownBy(() -> payment.complete("pg-txn-002"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── 결제 실패 ──

    @Nested
    @DisplayName("결제 실패")
    class Failure {

        @Test
        @DisplayName("결제 실패 시 FAILED 상태가 된다")
        void fail_changeStatusToFailed() {
            Payment payment = createPayment();

            payment.fail("카드 한도 초과");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("실패 시 PaymentFailed 이벤트가 발행된다")
        void fail_publishesEvent() {
            Payment payment = createPayment();

            payment.fail("잔액 부족");

            assertThat(payment.getEvents()).hasSize(1);
            PaymentFailed event = (PaymentFailed) payment.getEvents().get(0);
            assertThat(event.getReason()).isEqualTo("잔액 부족");
        }
    }

    // ── 환불 ──

    @Nested
    @DisplayName("환불")
    class Refunding {

        @Test
        @DisplayName("부분 환불 시 PARTIALLY_REFUNDED 상태가 된다")
        void partialRefund() {
            Payment payment = createCompletedPayment();

            payment.refund(Money.krw(30000), "상품 불량");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(payment.getTotalRefundedAmount()).isEqualTo(Money.krw(30000));
            assertThat(payment.getRefunds()).hasSize(1);
        }

        @Test
        @DisplayName("전체 환불 시 FULLY_REFUNDED 상태가 된다")
        void fullRefund() {
            Payment payment = createCompletedPayment();

            payment.refund(Money.krw(100000), "주문 취소");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FULLY_REFUNDED);
        }

        @Test
        @DisplayName("여러 번 부분 환불 가능")
        void multiplePartialRefunds() {
            Payment payment = createCompletedPayment();

            payment.refund(Money.krw(30000), "1차 반품");
            payment.refund(Money.krw(20000), "2차 반품");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(payment.getTotalRefundedAmount()).isEqualTo(Money.krw(50000));
            assertThat(payment.getRefunds()).hasSize(2);
        }

        @Test
        @DisplayName("환불 총액이 결제 금액을 초과하면 예외 발생")
        void refund_exceedsTotalAmount_throwsException() {
            Payment payment = createCompletedPayment();
            payment.refund(Money.krw(80000), "1차");

            assertThatThrownBy(() -> payment.refund(Money.krw(30000), "2차"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("초과");
        }

        @Test
        @DisplayName("환불 시 RefundCompleted 이벤트가 발행된다")
        void refund_publishesEvent() {
            Payment payment = createCompletedPayment();

            payment.refund(Money.krw(50000), "반품");

            assertThat(payment.getEvents()).hasSize(1);
            assertThat(payment.getEvents().get(0)).isInstanceOf(RefundCompleted.class);
        }

        @Test
        @DisplayName("INITIATED 상태에서는 환불 불가 (매입 전이니까)")
        void refund_fromInitiated_throwsException() {
            Payment payment = createPayment();

            assertThatThrownBy(() -> payment.refund(Money.krw(50000), "취소"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
