package com.ecommerce.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public class CategoryId {

    private final String id;

    public CategoryId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("카테고리 ID는 필수입니다");
        }
        this.id = id;
    }

    public static CategoryId generate() {
        return new CategoryId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryId that = (CategoryId) o;
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
