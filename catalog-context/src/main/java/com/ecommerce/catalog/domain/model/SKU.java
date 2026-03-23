package com.ecommerce.catalog.domain.model;

import com.ecommerce.common.model.Money;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * SKU (Stock Keeping Unit) Entity.
 *
 * SKU는 옵션 조합으로 만들어지는 실제 구매 단위.
 * 예: "에어맥스 90 / 270mm / 블랙" = 하나의 SKU
 *
 * Entity인 이유: 고유 ID가 있고, 가격이 변경될 수 있다.
 * Product Aggregate에 속하므로 Product를 통해서만 접근.
 *
 * optionCombination: 어떤 옵션 조합인지를 나타냄
 *   예: { "사이즈 옵션ID" → "270", "컬러 옵션ID" → "블랙" }
 */
public class SKU {

    private final SkuId skuId;
    private final Map<String, OptionValue> optionCombination;
    private Money price;

    public SKU(SkuId skuId, Map<String, OptionValue> optionCombination, Money price) {
        if (skuId == null) {
            throw new IllegalArgumentException("SKU ID는 필수입니다");
        }
        if (optionCombination == null || optionCombination.isEmpty()) {
            throw new IllegalArgumentException("옵션 조합은 필수입니다");
        }
        if (price == null || !price.isPositive()) {
            throw new IllegalArgumentException("가격은 양수여야 합니다");
        }
        this.skuId = skuId;
        this.optionCombination = Map.copyOf(optionCombination);
        this.price = price;
    }

    /**
     * 다른 SKU와 옵션 조합이 같은지 확인.
     * 중복 SKU 방지용.
     */
    public boolean hasSameOptionCombination(Map<String, OptionValue> other) {
        return this.optionCombination.equals(other);
    }

    public void changePrice(Money newPrice) {
        if (newPrice == null || !newPrice.isPositive()) {
            throw new IllegalArgumentException("가격은 양수여야 합니다");
        }
        this.price = newPrice;
    }

    public SkuId getSkuId() {
        return skuId;
    }

    public Map<String, OptionValue> getOptionCombination() {
        return Collections.unmodifiableMap(optionCombination);
    }

    public Money getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SKU sku = (SKU) o;
        return skuId.equals(sku.skuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skuId);
    }
}
