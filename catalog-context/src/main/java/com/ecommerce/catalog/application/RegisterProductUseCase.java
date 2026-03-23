package com.ecommerce.catalog.application;

import com.ecommerce.catalog.domain.model.*;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import com.ecommerce.common.model.Money;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상품 등록 유스케이스 (Application Service).
 *
 * Application Service의 역할:
 * 1. 외부 요청(DTO)을 도메인 객체로 변환
 * 2. 도메인 객체에 일을 시킴 (직접 비즈니스 로직을 갖지 않음!)
 * 3. Repository를 통해 저장
 * 4. 도메인 이벤트를 발행
 *
 * 주의: "Application Service는 얇아야 한다"
 * - 비즈니스 로직은 도메인 모델(Product, SKU)에 있음
 * - 여기서는 오케스트레이션(조율)만 담당
 */
public class RegisterProductUseCase {

    private final ProductRepository productRepository;

    public RegisterProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductId execute(RegisterProductCommand command) {
        // 1. 도메인 객체 생성 (팩토리 메서드 사용)
        Product product = Product.register(
                command.name(),
                command.description(),
                new SellerId(command.sellerId())
        );

        // 2. 옵션 추가
        for (OptionCommand option : command.options()) {
            List<OptionValue> values = option.values().stream()
                    .map(OptionValue::new)
                    .collect(Collectors.toList());
            product.addOption(new ProductOption(option.name(), values));
        }

        // 3. SKU 추가
        for (SkuCommand skuCmd : command.skus()) {
            Map<String, OptionValue> combination = skuCmd.optionCombination().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new OptionValue(e.getValue())
                    ));
            SKU sku = new SKU(SkuId.generate(), combination, Money.krw(skuCmd.price()));
            product.addSku(sku);
        }

        // 4. 저장
        productRepository.save(product);

        // TODO: 도메인 이벤트 발행 (Phase 2에서 재고 연동 시 구현)

        return product.getProductId();
    }

    // ── Command (입력 DTO) ──
    // record: Java 16+의 불변 데이터 클래스. getter/equals/hashCode 자동 생성.

    public record RegisterProductCommand(
            String name,
            String description,
            String sellerId,
            List<OptionCommand> options,
            List<SkuCommand> skus
    ) {}

    public record OptionCommand(
            String name,
            List<String> values
    ) {}

    public record SkuCommand(
            Map<String, String> optionCombination,
            long price
    ) {}
}
