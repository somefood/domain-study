package com.ecommerce.payment.application;

import com.ecommerce.common.model.Money;
import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentGateway;
import com.ecommerce.payment.domain.model.PaymentGateway.PaymentGatewayException;
import com.ecommerce.payment.domain.model.PaymentMethod;
import com.ecommerce.payment.domain.repository.PaymentRepository;

/**
 * ★ 주문-결제 Saga (Process Manager) ★
 *
 * Saga란?
 * 여러 Bounded Context에 걸친 작업을 순서대로 실행하고,
 * 중간에 실패하면 이전 단계를 되돌리는(보상 트랜잭션) 패턴.
 *
 * 이 Saga가 조율하는 흐름:
 *
 *   [성공 경로]
 *   1. 재고 예약 (InventoryService)
 *   2. PG 결제 (PaymentGateway)
 *   3. 결제 완료 → 재고 예약 확정 → 주문 PAID
 *
 *   [실패 경로 - 결제 실패]
 *   1. 재고 예약 ✅
 *   2. PG 결제 ❌
 *   3. 보상: 재고 예약 취소 → 주문 CANCELLED
 *
 *   [실패 경로 - 재고 부족]
 *   1. 재고 예약 ❌
 *   2. 주문 CANCELLED (보상할 것 없음)
 *
 * 왜 Application Service인가?
 * - 도메인 로직(결제 상태 전이)은 Payment Aggregate에 있음
 * - Saga는 여러 서비스를 "조율"하는 역할만 함 → Application Layer
 *
 * 현재 구현:
 * - 재고/주문 서비스는 인터페이스로 추상화 (다른 컨텍스트이므로)
 * - 같은 프로세스 내 동기 호출 (나중에 이벤트 기반 비동기로 전환 가능)
 */
public class OrderPaymentSaga {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final InventoryAclPort inventoryPort;
    private final OrderAclPort orderPort;

    public OrderPaymentSaga(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            InventoryAclPort inventoryPort,
            OrderAclPort orderPort
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.inventoryPort = inventoryPort;
        this.orderPort = orderPort;
    }

    /**
     * 주문이 생성되었을 때 호출되는 Saga 진입점.
     *
     * 실제로는 OrderPlaced 이벤트를 구독하여 자동으로 호출되지만,
     * 학습 편의상 직접 호출하는 형태로 구현.
     */
    public SagaResult processOrder(String orderId, String skuId, int quantity, Money amount) {
        // ── 단계 1: 재고 예약 ──
        boolean reserved;
        try {
            reserved = inventoryPort.reserve(skuId, orderId, quantity);
        } catch (Exception e) {
            reserved = false;
        }

        if (!reserved) {
            // 재고 부족 → 주문 취소 (보상할 것 없음, 아직 아무것도 안 했으니까)
            orderPort.cancel(orderId, "재고 부족");
            return SagaResult.failure("재고 부족");
        }

        // ── 단계 2: 결제 시도 ──
        Payment payment = Payment.initiate(orderId, amount, PaymentMethod.CARD, "key-" + orderId);

        try {
            String pgTxnId = paymentGateway.authorize(orderId, amount, PaymentMethod.CARD);
            payment.complete(pgTxnId);
            paymentRepository.save(payment);

            // ── 단계 3: 성공 → 재고 확정 + 주문 PAID ──
            inventoryPort.confirmReservation(orderId);
            orderPort.pay(orderId, payment.getPaymentId().getId(), amount);

            return SagaResult.success(payment.getPaymentId().getId());

        } catch (PaymentGatewayException e) {
            // ── 결제 실패 → 보상 트랜잭션 ──
            payment.fail(e.getMessage());
            paymentRepository.save(payment);

            // 보상 1: 재고 예약 취소 (이미 예약했으니 되돌려야 함)
            inventoryPort.cancelReservation(orderId, "결제 실패");

            // 보상 2: 주문 취소
            orderPort.cancel(orderId, "결제 실패: " + e.getMessage());

            return SagaResult.failure("결제 실패: " + e.getMessage());
        }
    }

    // ── 다른 컨텍스트와의 ACL 포트 ──
    // 재고/주문 컨텍스트를 직접 import하지 않고 인터페이스로 추상화

    /**
     * ★ 재고 컨텍스트 ACL ★
     * 결제 컨텍스트가 재고 컨텍스트에 요청할 때 사용하는 포트.
     * 구현체는 인프라 레이어에서 재고 컨텍스트의 서비스를 호출.
     */
    public interface InventoryAclPort {
        boolean reserve(String skuId, String orderId, int quantity);
        void confirmReservation(String orderId);
        void cancelReservation(String orderId, String reason);
    }

    /**
     * ★ 주문 컨텍스트 ACL ★
     * 결제 컨텍스트가 주문 컨텍스트에 요청할 때 사용하는 포트.
     */
    public interface OrderAclPort {
        void pay(String orderId, String paymentId, Money amount);
        void cancel(String orderId, String reason);
    }

    /**
     * Saga 실행 결과.
     */
    public record SagaResult(boolean success, String message) {
        public static SagaResult success(String paymentId) {
            return new SagaResult(true, "결제 완료: " + paymentId);
        }
        public static SagaResult failure(String reason) {
            return new SagaResult(false, reason);
        }
    }
}
