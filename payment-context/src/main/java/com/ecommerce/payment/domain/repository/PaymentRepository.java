package com.ecommerce.payment.domain.repository;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentId;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId paymentId);

    Optional<Payment> findByOrderId(String orderId);

    /**
     * 멱등성 키로 기존 결제를 조회.
     * 같은 키로 이미 결제가 존재하면 중복 결제를 방지.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
