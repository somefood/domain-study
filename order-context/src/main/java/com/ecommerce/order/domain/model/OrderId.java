package com.ecommerce.order.domain.model;

import java.util.Objects;
import java.util.UUID;

public class OrderId {

    private final String id;

    public OrderId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("주문 ID는 필수입니다");
        }
        this.id = id;
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    public String getId() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return id.equals(orderId.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id; }
}
