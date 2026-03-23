package com.ecommerce.catalog.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 상품 옵션 Entity.
 *
 * Entity인 이유: 옵션에는 고유한 ID가 있고, 옵션값을 추가/삭제할 수 있다 (상태 변경).
 * Product Aggregate 안에 속하므로 독립적으로 존재할 수 없다.
 *
 * 예: "사이즈" 옵션 → values: [270, 275, 280]
 *     "컬러" 옵션 → values: [블랙, 화이트, 네이비]
 */
public class ProductOption {

    private final String optionId;
    private final String name;
    private final List<OptionValue> values;

    public ProductOption(String name, List<OptionValue> values) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("옵션 이름은 필수입니다");
        }
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("옵션값은 최소 1개 이상이어야 합니다");
        }
        this.optionId = UUID.randomUUID().toString();
        this.name = name;
        this.values = new ArrayList<>(values);
    }

    public String getOptionId() {
        return optionId;
    }

    public String getName() {
        return name;
    }

    public List<OptionValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductOption that = (ProductOption) o;
        return optionId.equals(that.optionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionId);
    }
}
