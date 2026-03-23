package com.ecommerce.catalog.application;

import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.repository.ProductRepository;

/**
 * 상품 활성화 유스케이스.
 *
 * 비즈니스 로직("SKU가 없으면 활성화 불가")은 Product에 있다.
 * 여기서는 Product를 찾아서 activate()를 호출하는 것만 한다.
 */
public class ActivateProductUseCase {

    private final ProductRepository productRepository;

    public ActivateProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(String productIdValue) {
        ProductId productId = new ProductId(productIdValue);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productIdValue));

        product.activate();
        productRepository.save(product);
    }
}
