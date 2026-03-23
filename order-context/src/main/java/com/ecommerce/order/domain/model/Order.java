package com.ecommerce.order.domain.model;

import com.ecommerce.common.model.Address;
import com.ecommerce.common.model.AggregateRoot;
import com.ecommerce.common.model.Money;
import com.ecommerce.order.domain.event.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 주문 Aggregate Root — 이커머스의 심장.
 *
 * 핵심 특징:
 * 1. 상태 머신: 각 메서드(pay, prepare, ship...)가 상태 전이를 수행하며,
 *    현재 상태에서 허용되지 않는 전이는 예외를 던진다.
 * 2. 가격 스냅샷: OrderLine의 unitPrice는 주문 시점에 캡처된 값.
 *    카탈로그에서 가격이 바뀌어도 주문의 가격은 불변.
 * 3. 다른 컨텍스트와 ID로만 참조: ordererId(회원), skuId(카탈로그)
 *
 * 불변식:
 * - 주문항목은 최소 1개 이상
 * - 상태 전이 규칙을 반드시 따름
 * - 배송 후에는 취소 불가 (반품으로 처리)
 */
public class Order extends AggregateRoot {

    private final OrderId orderId;
    private final String ordererId;         // 회원 컨텍스트의 ID만 참조
    private final List<OrderLine> orderLines;
    private final Address shippingAddress;  // 스냅샷 (배송 후 회원이 주소를 바꿔도 무관)
    private OrderStatus status;
    private final LocalDateTime orderedAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    /**
     * 주문 생성 (팩토리 메서드).
     *
     * @param ordererId 주문자 ID (회원 컨텍스트 참조)
     * @param orderLines 주문 항목 목록 (ACL을 통해 스냅샷된 가격 포함)
     * @param shippingAddress 배송지 (스냅샷)
     */
    public static Order place(String ordererId, List<OrderLine> orderLines, Address shippingAddress) {
        if (ordererId == null || ordererId.isBlank()) {
            throw new IllegalArgumentException("주문자 ID는 필수입니다");
        }
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 최소 1개 이상이어야 합니다");
        }
        if (shippingAddress == null) {
            throw new IllegalArgumentException("배송지는 필수입니다");
        }

        Order order = new Order(ordererId, orderLines, shippingAddress);
        order.registerEvent(new OrderPlaced(
                order.orderId.getId(), ordererId, order.getTotalAmount()
        ));
        return order;
    }

    private Order(String ordererId, List<OrderLine> orderLines, Address shippingAddress) {
        this.orderId = OrderId.generate();
        this.ordererId = ordererId;
        this.orderLines = new ArrayList<>(orderLines);
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.CREATED;
        this.orderedAt = LocalDateTime.now();
    }

    // ── 상태 전이 메서드 ──
    // 각 메서드가 "현재 상태 검증 → 상태 변경 → 이벤트 발행" 패턴을 따른다

    /**
     * 결제 완료 처리.
     * CREATED → PAID
     */
    public void pay(String paymentId, Money paidAmount) {
        validateStatusTransition(OrderStatus.CREATED, "결제");
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
        registerEvent(new OrderPaid(orderId.getId(), paymentId, paidAmount));
    }

    /**
     * 상품 준비 시작.
     * PAID → PREPARING
     */
    public void prepare() {
        validateStatusTransition(OrderStatus.PAID, "준비");
        this.status = OrderStatus.PREPARING;
    }

    /**
     * 출고 (배송 시작).
     * PREPARING → SHIPPED
     */
    public void ship(String trackingNumber) {
        validateStatusTransition(OrderStatus.PREPARING, "출고");
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("운송장 번호는 필수입니다");
        }
        this.status = OrderStatus.SHIPPED;
        registerEvent(new OrderShipped(orderId.getId(), trackingNumber));
    }

    /**
     * 배송 완료.
     * SHIPPED → DELIVERED
     */
    public void deliver() {
        validateStatusTransition(OrderStatus.SHIPPED, "배송 완료");
        this.status = OrderStatus.DELIVERED;
        registerEvent(new OrderDelivered(orderId.getId(), LocalDateTime.now()));
    }

    /**
     * 구매 확정.
     * DELIVERED → COMPLETED
     */
    public void complete() {
        validateStatusTransition(OrderStatus.DELIVERED, "구매 확정");
        this.status = OrderStatus.COMPLETED;
        registerEvent(new OrderCompleted(orderId.getId(), LocalDateTime.now()));
    }

    /**
     * 주문 취소.
     *
     * CREATED, PAID, PREPARING 상태에서만 가능.
     * SHIPPED 이후에는 취소 불가 → 반품(Phase 6)으로 처리해야 함.
     */
    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED
                || status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    String.format("현재 상태에서는 취소할 수 없습니다: %s. 배송 후에는 반품으로 처리하세요.", status)
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
        registerEvent(new OrderCancelled(orderId.getId(), reason));
    }

    // ── 조회 ──

    /**
     * 주문 총액.
     * 모든 주문항목의 소계(lineTotal)를 합산.
     */
    public Money getTotalAmount() {
        return orderLines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(Money.krw(0), Money::add);
    }

    private void validateStatusTransition(OrderStatus expectedCurrent, String action) {
        if (this.status != expectedCurrent) {
            throw new IllegalStateException(
                    String.format("%s 상태에서만 %s할 수 있습니다. 현재 상태: %s",
                            expectedCurrent, action, this.status)
            );
        }
    }

    // Getters
    public OrderId getOrderId() { return orderId; }
    public String getOrdererId() { return ordererId; }
    public List<OrderLine> getOrderLines() { return Collections.unmodifiableList(orderLines); }
    public Address getShippingAddress() { return shippingAddress; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
}
