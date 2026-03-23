package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;

import java.util.Optional;

/**
 * 상품 Repository 인터페이스 (포트).
 *
 * DDD에서 Repository는 "도메인 레이어에 인터페이스, 인프라 레이어에 구현".
 * 이렇게 하면 도메인 로직이 DB 기술(JPA, MyBatis 등)에 의존하지 않는다.
 *
 * 도메인 레이어: "나는 Product를 저장하고 조회할 수 있어야 해" (인터페이스)
 * 인프라 레이어: "JPA로 구현할게" 또는 "인메모리로 구현할게" (구현체)
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId productId);
}
