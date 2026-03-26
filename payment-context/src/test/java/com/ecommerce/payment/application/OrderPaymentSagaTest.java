package com.ecommerce.payment.application;

import com.ecommerce.common.model.Money;
import com.ecommerce.payment.application.OrderPaymentSaga.*;
import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.infrastructure.FakePaymentGateway;
import com.ecommerce.payment.infrastructure.InMemoryPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 주문-결제 Saga 테스트.
 *
 * Saga의 3가지 시나리오를 검증:
 * 1. 성공: 재고 예약 → 결제 → 재고 확정 → 주문 PAID
 * 2. 결제 실패: 재고 예약 → 결제 실패 → 재고 예약 취소(보상) → 주문 CANCELLED
 * 3. 재고 부족: 재고 예약 실패 → 주문 CANCELLED
 */
class OrderPaymentSagaTest {

    private InMemoryPaymentRepository paymentRepository;
    private FakePaymentGateway paymentGateway;
    private SpyInventoryPort inventoryPort;
    private SpyOrderPort orderPort;
    private OrderPaymentSaga saga;

    @BeforeEach
    void setUp() {
        paymentRepository = new InMemoryPaymentRepository();
        paymentGateway = new FakePaymentGateway();
        inventoryPort = new SpyInventoryPort();
        orderPort = new SpyOrderPort();
        saga = new OrderPaymentSaga(paymentRepository, paymentGateway, inventoryPort, orderPort);
    }

    @Nested
    @DisplayName("성공 경로")
    class HappyPath {

        @Test
        @DisplayName("재고 예약 → 결제 성공 → 재고 확정 → 주문 PAID")
        void success_flow() {
            SagaResult result = saga.processOrder("order-1", "sku-1", 2, Money.krw(100000));

            // Saga 성공
            assertThat(result.success()).isTrue();

            // 재고: 예약 → 확정 호출됨
            assertThat(inventoryPort.reserveCalled).isTrue();
            assertThat(inventoryPort.confirmCalled).isTrue();
            assertThat(inventoryPort.cancelCalled).isFalse(); // 취소는 안 됨

            // 주문: pay 호출됨
            assertThat(orderPort.payCalled).isTrue();
            assertThat(orderPort.cancelCalled).isFalse(); // 취소는 안 됨

            // 결제: CAPTURED 상태로 저장됨
            Payment payment = paymentRepository.findByOrderId("order-1").orElseThrow();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        }
    }

    @Nested
    @DisplayName("실패 경로 - 결제 실패")
    class PaymentFailurePath {

        @Test
        @DisplayName("재고 예약 → 결제 실패 → 보상: 재고 예약 취소 + 주문 취소")
        void paymentFailure_compensates() {
            // PG가 실패하도록 설정
            paymentGateway.setNextPaymentToFail();

            SagaResult result = saga.processOrder("order-1", "sku-1", 2, Money.krw(100000));

            // Saga 실패
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("결제 실패");

            // 재고: 예약 후 취소됨 (보상 트랜잭션)
            assertThat(inventoryPort.reserveCalled).isTrue();
            assertThat(inventoryPort.cancelCalled).isTrue();  // ← 보상!
            assertThat(inventoryPort.confirmCalled).isFalse();

            // 주문: 취소됨 (보상 트랜잭션)
            assertThat(orderPort.cancelCalled).isTrue();      // ← 보상!
            assertThat(orderPort.payCalled).isFalse();

            // 결제: FAILED 상태로 저장됨
            Payment payment = paymentRepository.findByOrderId("order-1").orElseThrow();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("실패 경로 - 재고 부족")
    class InventoryFailurePath {

        @Test
        @DisplayName("재고 예약 실패 → 주문 취소 (보상할 것 없음)")
        void inventoryFailure_cancelsOrder() {
            // 재고 예약이 실패하도록 설정
            inventoryPort.setShouldFail(true);

            SagaResult result = saga.processOrder("order-1", "sku-1", 2, Money.krw(100000));

            // Saga 실패
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("재고 부족");

            // 재고: 예약 시도만 하고, 확정/취소는 안 함
            assertThat(inventoryPort.reserveCalled).isTrue();
            assertThat(inventoryPort.confirmCalled).isFalse();
            assertThat(inventoryPort.cancelCalled).isFalse(); // 예약 자체가 안 됐으니 취소할 것도 없음

            // 주문: 취소됨
            assertThat(orderPort.cancelCalled).isTrue();
            assertThat(orderPort.cancelReason).isEqualTo("재고 부족");

            // 결제: 시도조차 안 됨
            assertThat(paymentRepository.findByOrderId("order-1")).isEmpty();
        }
    }

    // ── Spy 구현체 (호출 여부를 기록하는 테스트용) ──

    static class SpyInventoryPort implements InventoryAclPort {
        boolean reserveCalled = false;
        boolean confirmCalled = false;
        boolean cancelCalled = false;
        private boolean shouldFail = false;

        void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public boolean reserve(String skuId, String orderId, int quantity) {
            reserveCalled = true;
            return !shouldFail;
        }

        @Override
        public void confirmReservation(String orderId) {
            confirmCalled = true;
        }

        @Override
        public void cancelReservation(String orderId, String reason) {
            cancelCalled = true;
        }
    }

    static class SpyOrderPort implements OrderAclPort {
        boolean payCalled = false;
        boolean cancelCalled = false;
        String cancelReason;

        @Override
        public void pay(String orderId, String paymentId, Money amount) {
            payCalled = true;
        }

        @Override
        public void cancel(String orderId, String reason) {
            cancelCalled = true;
            cancelReason = reason;
        }
    }
}
