package com.ecommerce.payment.infrastructure;

import com.ecommerce.common.model.Money;
import com.ecommerce.payment.domain.model.PaymentGateway;
import com.ecommerce.payment.domain.model.PaymentMethod;

import java.util.UUID;

/**
 * 가짜 PG 구현체 (테스트/학습용).
 *
 * 실제 PG에서는:
 * - 카드 번호 검증, 3D Secure 인증, 한도 확인 등이 일어남
 * - 네트워크 타임아웃, 중복 요청 등의 예외 상황 처리 필요
 *
 * 여기서는 항상 성공하거나, 특정 조건에서 실패를 시뮬레이션.
 */
public class FakePaymentGateway implements PaymentGateway {

    private boolean shouldFail = false;

    /**
     * 다음 결제를 실패하게 설정 (테스트용).
     */
    public void setNextPaymentToFail() {
        this.shouldFail = true;
    }

    @Override
    public String authorize(String orderId, Money amount, PaymentMethod method) {
        if (shouldFail) {
            shouldFail = false;
            throw new PaymentGatewayException("PG 결제 승인 실패 (시뮬레이션)");
        }

        // 성공: PG 트랜잭션 ID 반환
        return "pg-txn-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void refund(String pgTransactionId, Money amount) {
        // 가짜 환불: 항상 성공
    }
}
