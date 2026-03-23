package com.ecommerce.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public class InventoryItemId {

    private final String id;

    public InventoryItemId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("재고 아이템 ID는 필수입니다");
        }
        this.id = id;
    }

    public static InventoryItemId generate() {
        return new InventoryItemId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItemId that = (InventoryItemId) o;
        return id.equals(that.id);
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
