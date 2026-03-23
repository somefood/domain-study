package com.ecommerce.catalog.domain.model;

import com.ecommerce.catalog.domain.event.PriceChanged;
import com.ecommerce.catalog.domain.event.ProductActivated;
import com.ecommerce.catalog.domain.event.ProductDiscontinued;
import com.ecommerce.catalog.domain.event.ProductRegistered;
import com.ecommerce.common.model.AggregateRoot;
import com.ecommerce.common.model.Money;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 상품 Aggregate Root.
 *
 * 이 클래스가 Aggregate Root인 이유:
 * - Product 안에 Option, SKU가 포함되어 있는데, 이들은 Product 없이는 의미가 없음
 * - SKU 가격을 바꾸려면 반드시 Product를 통해야 함 (일관성 보장)
 * - 외부에서는 ProductId로만 이 Aggregate를 참조
 *
 * 핵심 불변식 (Invariants):
 * 1. SKU가 0개인 상품은 활성화할 수 없다
 * 2. 같은 옵션 조합의 SKU가 중복될 수 없다
 * 3. 상태 전이 규칙: DRAFT→ACTIVE→DISCONTINUED (DRAFT→DISCONTINUED 불가)
 * 4. 모든 SKU의 가격은 양수여야 한다
 */
public class Product extends AggregateRoot {

    private final ProductId productId;
    private String name;
    private String description;
    private ProductStatus status;
    private final SellerId sellerId;
    private final Set<CategoryId> categoryIds;
    private final List<ProductOption> options;
    private final List<SKU> skus;

    /**
     * 상품 등록 (팩토리 메서드).
     *
     * 왜 생성자 대신 팩토리 메서드를 쓰는가?
     * - "상품을 등록한다"는 도메인 행위를 표현
     * - 등록 시 ProductRegistered 이벤트를 발행해야 하는데,
     *   생성자에서 이벤트를 발행하면 의미가 불명확
     */
    public static Product register(String name, String description, SellerId sellerId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("셀러 ID는 필수입니다");
        }

        Product product = new Product(ProductId.generate(), name, description, sellerId);
        product.registerEvent(new ProductRegistered(
                product.productId.getId(), name, sellerId.getId()
        ));
        return product;
    }

    private Product(ProductId productId, String name, String description, SellerId sellerId) {
        this.productId = productId;
        this.name = name;
        this.description = description != null ? description : "";
        this.status = ProductStatus.DRAFT;  // 새 상품은 항상 DRAFT로 시작
        this.sellerId = sellerId;
        this.categoryIds = new HashSet<>();
        this.options = new ArrayList<>();
        this.skus = new ArrayList<>();
    }

    /**
     * 옵션 추가.
     * DRAFT 상태에서만 가능 (이미 판매 중인 상품의 옵션 구조를 바꾸면 위험)
     */
    public void addOption(ProductOption option) {
        if (status != ProductStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 옵션을 추가할 수 있습니다");
        }
        options.add(option);
    }

    /**
     * SKU 추가.
     *
     * 검증:
     * 1. 단종 상태면 불가
     * 2. SKU의 옵션 키가 Product에 정의된 옵션 이름과 정확히 일치해야 함
     * 3. SKU의 옵션값이 해당 옵션에 정의된 값 중 하나여야 함
     * 4. 같은 옵션 조합의 SKU가 이미 있으면 불가
     */
    public void addSku(SKU sku) {
        if (status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("단종된 상품에는 SKU를 추가할 수 없습니다");
        }

        validateSkuOptionCombination(sku);

        boolean duplicate = skus.stream()
                .anyMatch(existing -> existing.hasSameOptionCombination(sku.getOptionCombination()));
        if (duplicate) {
            throw new IllegalArgumentException("동일한 옵션 조합의 SKU가 이미 존재합니다");
        }

        skus.add(sku);
    }

    /**
     * SKU의 옵션 조합이 Product에 정의된 옵션과 일치하는지 검증.
     *
     * 예: Product 옵션이 [사이즈(270,280), 컬러(블랙,화이트)]라면
     *     SKU는 반드시 {사이즈=270|280, 컬러=블랙|화이트} 형태여야 함
     */
    private void validateSkuOptionCombination(SKU sku) {
        Map<String, OptionValue> combination = sku.getOptionCombination();

        // 옵션이 정의되어 있지 않으면 검증 스킵 (옵션 없는 단일 상품)
        if (options.isEmpty()) {
            return;
        }

        // 1. SKU의 옵션 키 개수와 Product의 옵션 개수가 같아야 함
        Set<String> definedOptionNames = options.stream()
                .map(ProductOption::getName)
                .collect(Collectors.toSet());

        if (!definedOptionNames.equals(combination.keySet())) {
            throw new IllegalArgumentException(
                    String.format("SKU 옵션 조합이 상품 옵션과 일치하지 않습니다. 필요: %s, 입력: %s",
                            definedOptionNames, combination.keySet())
            );
        }

        // 2. 각 옵션값이 해당 옵션에 정의된 값 중 하나인지 확인
        for (ProductOption option : options) {
            OptionValue skuValue = combination.get(option.getName());
            boolean validValue = option.getValues().contains(skuValue);
            if (!validValue) {
                throw new IllegalArgumentException(
                        String.format("옵션 '%s'에 유효하지 않은 값입니다: %s. 허용 값: %s",
                                option.getName(), skuValue, option.getValues())
                );
            }
        }
    }

    /**
     * 상품 활성화 (판매 시작).
     *
     * 불변식: SKU가 최소 1개 있어야 활성화 가능.
     * 팔 수 있는 게 없는데 활성화하면 안 되니까.
     */
    public void activate() {
        if (status != ProductStatus.DRAFT) {
            throw new IllegalStateException(
                    String.format("DRAFT 상태에서만 활성화할 수 있습니다. 현재 상태: %s", status)
            );
        }
        if (skus.isEmpty()) {
            throw new IllegalStateException("SKU가 최소 1개 이상이어야 활성화할 수 있습니다");
        }

        this.status = ProductStatus.ACTIVE;

        List<String> skuIds = skus.stream()
                .map(sku -> sku.getSkuId().getId())
                .collect(Collectors.toList());
        registerEvent(new ProductActivated(productId.getId(), skuIds));
    }

    /**
     * 상품 단종.
     *
     * ACTIVE 상태에서만 가능. DRAFT→DISCONTINUED는 불가.
     * (판매한 적 없는 상품은 "단종"이 아니라 그냥 삭제)
     */
    public void discontinue() {
        if (status != ProductStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("ACTIVE 상태에서만 단종할 수 있습니다. 현재 상태: %s", status)
            );
        }

        this.status = ProductStatus.DISCONTINUED;
        registerEvent(new ProductDiscontinued(productId.getId()));
    }

    /**
     * SKU 가격 변경.
     *
     * Product(Aggregate Root)를 통해서만 가격을 바꿀 수 있다.
     * SKU를 직접 찾아서 바꾸는 게 아님 → Aggregate 일관성 보장.
     */
    public void changeSkuPrice(SkuId skuId, Money newPrice) {
        SKU sku = findSku(skuId);
        Money oldPrice = sku.getPrice();
        sku.changePrice(newPrice);
        registerEvent(new PriceChanged(
                productId.getId(), skuId.getId(), oldPrice, newPrice
        ));
    }

    public void addCategory(CategoryId categoryId) {
        categoryIds.add(categoryId);
    }

    public void removeCategory(CategoryId categoryId) {
        categoryIds.remove(categoryId);
    }

    private SKU findSku(SkuId skuId) {
        return skus.stream()
                .filter(sku -> sku.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("SKU를 찾을 수 없습니다: " + skuId));
    }

    // Getters
    public ProductId getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public SellerId getSellerId() {
        return sellerId;
    }

    public Set<CategoryId> getCategoryIds() {
        return Collections.unmodifiableSet(categoryIds);
    }

    public List<ProductOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public List<SKU> getSkus() {
        return Collections.unmodifiableList(skus);
    }
}
