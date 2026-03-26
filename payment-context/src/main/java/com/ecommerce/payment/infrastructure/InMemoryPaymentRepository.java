package com.ecommerce.payment.infrastructure;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentId;
import com.ecommerce.payment.domain.repository.PaymentRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<PaymentId, Payment> store = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.getPaymentId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return Optional.ofNullable(store.get(paymentId));
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return store.values().stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst();
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return store.values().stream()
                .filter(p -> p.getIdempotencyKey().equals(idempotencyKey))
                .findFirst();
    }
}
