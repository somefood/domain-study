package com.ecommerce.common.model;

import java.util.Objects;

/**
 * 수량을 표현하는 Value Object.
 *
 * 왜 int가 아니라 별도 클래스로 만드는가?
 * - 수량은 음수가 될 수 없다는 도메인 규칙이 있다
 * - int를 그냥 쓰면 -5 같은 값이 들어와도 컴파일러가 잡아주지 않음
 * - Quantity 클래스로 감싸면 "생성 시점"에 바로 검증
 *
 * 이것이 DDD에서 말하는 "원시 타입 집착(Primitive Obsession)을 피하라"의 예시.
 * 도메인 개념에 맞는 타입을 만들면 버그가 줄어든다.
 */
public class Quantity {

    private final int value;

    public Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("수량은 음수일 수 없습니다: " + value);
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(this.value - other.value); // 음수면 생성자에서 예외 발생
    }

    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value >= other.value;
    }

    public boolean isZero() {
        return this.value == 0;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
