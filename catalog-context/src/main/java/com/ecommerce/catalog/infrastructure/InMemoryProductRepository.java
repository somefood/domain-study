package com.ecommerce.catalog.infrastructure;

import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.repository.ProductRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 Repository 구현.
 *
 * 테스트와 학습 목적. JPA 없이도 도메인 로직을 검증할 수 있다.
 * 나중에 JPA 구현체로 교체해도 도메인 로직에는 영향이 없다
 * → 이것이 포트/어댑터 패턴의 장점.
 */
public class InMemoryProductRepository implements ProductRepository {

    private final Map<ProductId, Product> store = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        store.put(product.getProductId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return Optional.ofNullable(store.get(productId));
    }
}
