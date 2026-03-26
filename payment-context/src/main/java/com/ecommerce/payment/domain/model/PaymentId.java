package com.ecommerce.payment.domain.model;

import java.util.Objects;
import java.util.UUID;

public class PaymentId {

    private final String id;

    public PaymentId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("결제 ID는 필수입니다");
        }
        this.id = id;
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID().toString());
    }

    public String getId() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentId that = (PaymentId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id; }
}
