package com.ecommerce.settlement.domain.policy;

import java.math.BigDecimal;

/**
 * 수수료 정책 (도메인 정책).
 *
 * DDD에서 "정책(Policy)"은 비즈니스 규칙을 캡슐화하는 패턴.
 * 실제로는 카테고리별, 셀러 등급별로 수수료율이 다를 수 있지만
 * 학습 목적으로 단일 비율(10%)을 사용.
 */
public class CommissionPolicy {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.10"); // 10%

    /**
     * 셀러의 수수료율을 반환.
     * 실제로는 sellerId를 기반으로 계약된 수수료율을 조회.
     */
    public BigDecimal getCommissionRate(String sellerId) {
        // 학습용: 모든 셀러에게 10% 수수료
        return DEFAULT_RATE;
    }
}
