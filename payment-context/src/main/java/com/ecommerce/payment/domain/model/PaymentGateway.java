package com.ecommerce.payment.domain.model;

import com.ecommerce.common.model.Money;

/**
 * PG(결제 대행사) 포트.
 *
 * 도메인 레이어에 인터페이스, 인프라 레이어에 구현체.
 * 토스페이먼츠, KG이니시스 등 어떤 PG를 쓰든 이 인터페이스만 구현하면 됨.
 */
public interface PaymentGateway {

    /**
     * PG에 결제 승인(인증+매입) 요청.
     * @return PG 트랜잭션 ID (성공 시)
     * @throws PaymentGatewayException 결제 실패 시
     */
    String authorize(String orderId, Money amount, PaymentMethod method);

    /**
     * PG에 환불 요청.
     */
    void refund(String pgTransactionId, Money amount);

    class PaymentGatewayException extends RuntimeException {
        public PaymentGatewayException(String message) {
            super(message);
        }
    }
}
