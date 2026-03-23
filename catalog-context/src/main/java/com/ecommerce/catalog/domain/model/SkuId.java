package com.ecommerce.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public class SkuId {

    private final String id;

    public SkuId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("SKU ID는 필수입니다");
        }
        this.id = id;
    }

    public static SkuId generate() {
        return new SkuId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkuId skuId = (SkuId) o;
        return id.equals(skuId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
