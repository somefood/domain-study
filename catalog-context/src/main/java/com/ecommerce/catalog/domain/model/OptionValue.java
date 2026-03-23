package com.ecommerce.catalog.domain.model;

import java.util.Objects;

/**
 * 옵션값 Value Object.
 *
 * 예: 사이즈 옵션의 "270", 컬러 옵션의 "블랙"
 * 값 자체가 의미이므로 Value Object.
 */
public class OptionValue {

    private final String value;

    public OptionValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("옵션값은 필수입니다");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionValue that = (OptionValue) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
