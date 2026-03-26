package com.ecommerce.settlement.domain.model;

import java.util.Objects;
import java.util.UUID;

public class SettlementId {

    private final String id;

    public SettlementId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("정산 ID는 필수입니다");
        }
        this.id = id;
    }

    public static SettlementId generate() {
        return new SettlementId(UUID.randomUUID().toString());
    }

    public String getId() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettlementId that = (SettlementId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id; }
}
