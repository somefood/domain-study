package com.ecommerce.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * 상품 식별자 Value Object.
 *
 * 왜 String이나 Long 대신 별도 타입을 만드는가?
 * - placeOrder(String productId, String memberId) → 실수로 순서를 바꿔도 컴파일 통과
 * - placeOrder(ProductId productId, MemberId memberId) → 순서 바꾸면 컴파일 에러!
 * - 타입 안전성(Type Safety)을 얻는다
 */
public class ProductId {

    private final String id;

    public ProductId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("상품 ID는 필수입니다");
        }
        this.id = id;
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return id.equals(productId.id);
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
