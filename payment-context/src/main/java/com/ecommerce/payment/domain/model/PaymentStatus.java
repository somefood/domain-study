package com.ecommerce.payment.domain.model;

/**
 * 결제 상태.
 *
 * INITIATED → AUTHORIZED → CAPTURED: 정상 플로우
 * INITIATED → FAILED: 결제 실패
 * AUTHORIZED → CANCELLED: 인증 후 취소 (매입 전)
 * CAPTURED → PARTIALLY_REFUNDED: 부분 환불
 * CAPTURED → FULLY_REFUNDED: 전체 환불
 */
public enum PaymentStatus {
    INITIATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    FULLY_REFUNDED
}
