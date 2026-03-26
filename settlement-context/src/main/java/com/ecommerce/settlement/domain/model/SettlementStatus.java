package com.ecommerce.settlement.domain.model;

/**
 * 정산 상태.
 *
 * PENDING → CALCULATED: 정산 금액이 계산됨
 * CALCULATED → APPROVED: 운영팀 승인
 * APPROVED → PAID: 셀러에게 실제 지급 완료
 */
public enum SettlementStatus {
    PENDING,
    CALCULATED,
    APPROVED,
    PAID
}
