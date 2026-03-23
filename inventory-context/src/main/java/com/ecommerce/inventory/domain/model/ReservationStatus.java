package com.ecommerce.inventory.domain.model;

/**
 * 예약 상태.
 *
 * PENDING → CONFIRMED: 결제 완료 시 예약 확정
 * PENDING → CANCELLED: 결제 실패 또는 주문 취소 시
 * PENDING → EXPIRED: 타임아웃 (예: 30분 내 결제 미완료)
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
