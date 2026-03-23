package com.ecommerce.order.domain.model;

/**
 * 주문 상태.
 *
 * 허용되는 전이:
 *   CREATED → PAID → PREPARING → SHIPPED → DELIVERED → COMPLETED
 *   CREATED → CANCELLED (결제 전 취소)
 *   PAID → CANCELLED (배송 전 취소)
 *   PREPARING → CANCELLED (배송 전 취소)
 *
 * 금지되는 전이:
 *   SHIPPED → CANCELLED (이미 출고됨, 반품으로 처리해야 함)
 *   CREATED → SHIPPED (결제 없이 배송 불가)
 *   등등...
 */
public enum OrderStatus {
    CREATED,
    PAID,
    PREPARING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED
}
