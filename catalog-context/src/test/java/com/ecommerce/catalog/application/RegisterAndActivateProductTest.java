package com.ecommerce.catalog.application;

import com.ecommerce.catalog.application.RegisterProductUseCase.*;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import com.ecommerce.catalog.infrastructure.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * 상품 등록 → 활성화 통합 테스트.
 *
 * Application Service를 통해 전체 플로우를 검증.
 * Spring 컨텍스트 없이 순수 Java로 테스트 가능
 * → 도메인 로직이 프레임워크에 의존하지 않는다는 증거.
 */
class RegisterAndActivateProductTest {

    private ProductRepository productRepository;
    private RegisterProductUseCase registerProductUseCase;
    private ActivateProductUseCase activateProductUseCase;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        registerProductUseCase = new RegisterProductUseCase(productRepository);
        activateProductUseCase = new ActivateProductUseCase(productRepository);
    }

    @Test
    @DisplayName("상품 등록 → 활성화 전체 플로우")
    void registerAndActivate() {
        // 1. 상품 등록
        RegisterProductCommand command = new RegisterProductCommand(
                "나이키 에어맥스 90",
                "편안한 운동화",
                "seller-1",
                List.of(new OptionCommand("사이즈", List.of("270", "280"))),
                List.of(
                        new SkuCommand(Map.of("사이즈", "270"), 139000),
                        new SkuCommand(Map.of("사이즈", "280"), 139000)
                )
        );

        ProductId productId = registerProductUseCase.execute(command);

        // 등록 직후 DRAFT 상태 확인
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getSkus()).hasSize(2);

        // 2. 활성화
        activateProductUseCase.execute(productId.getId());

        // 활성화 후 ACTIVE 상태 확인
        Product activated = productRepository.findById(productId).orElseThrow();
        assertThat(activated.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("SKU 없이 등록 → 활성화 시도 → 실패")
    void registerWithoutSkus_thenActivate_fails() {
        RegisterProductCommand command = new RegisterProductCommand(
                "빈 상품",
                "SKU 없음",
                "seller-1",
                List.of(),
                List.of()  // SKU 없음
        );

        ProductId productId = registerProductUseCase.execute(command);

        assertThatThrownBy(() -> activateProductUseCase.execute(productId.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SKU가 최소 1개");
    }

    @Test
    @DisplayName("존재하지 않는 상품 활성화 → 실패")
    void activateNonExistent_fails() {
        assertThatThrownBy(() -> activateProductUseCase.execute("non-existent-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }
}
