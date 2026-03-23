package com.ecommerce.catalog.domain.model;

import java.util.Objects;

/**
 * 셀러 식별자 Value Object.
 *
 * 셀러는 다른 Bounded Context에 속하므로 ID로만 참조한다.
 * 카탈로그 컨텍스트는 셀러의 이름이나 등급 같은 정보를 모른다.
 */
public class SellerId {

    private final String id;

    public SellerId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("셀러 ID는 필수입니다");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SellerId sellerId = (SellerId) o;
        return id.equals(sellerId.id);
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
