package com.ecommerce.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Quantity Value Object 테스트.
 *
 * "수량은 음수가 될 수 없다"는 도메인 규칙을 코드로 강제한다.
 * int를 그냥 쓰면 -5가 들어와도 모르지만,
 * Quantity를 쓰면 생성 시점에 바로 잡아낸다.
 */
class QuantityTest {

    @Test
    @DisplayName("같은 값의 Quantity는 동일하다 (값 동등성)")
    void equals_sameValue() {
        Quantity a = Quantity.of(5);
        Quantity b = Quantity.of(5);

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("음수로 생성하면 예외 발생")
    void constructor_negativeValue_throwsException() {
        assertThatThrownBy(() -> Quantity.of(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    @Test
    @DisplayName("0은 허용된다")
    void constructor_zero_isAllowed() {
        Quantity zero = Quantity.of(0);

        assertThat(zero.isZero()).isTrue();
        assertThat(zero.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("더하기 연산")
    void add() {
        Quantity result = Quantity.of(3).add(Quantity.of(2));

        assertThat(result).isEqualTo(Quantity.of(5));
    }

    @Test
    @DisplayName("빼기: 결과가 0 이상이면 성공")
    void subtract_validResult() {
        Quantity result = Quantity.of(5).subtract(Quantity.of(3));

        assertThat(result).isEqualTo(Quantity.of(2));
    }

    @Test
    @DisplayName("빼기: 결과가 음수면 예외 발생 (재고보다 많이 뺄 수 없다)")
    void subtract_negativeResult_throwsException() {
        assertThatThrownBy(() -> Quantity.of(3).subtract(Quantity.of(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("크기 비교")
    void isGreaterThanOrEqual() {
        assertThat(Quantity.of(5).isGreaterThanOrEqual(Quantity.of(3))).isTrue();
        assertThat(Quantity.of(3).isGreaterThanOrEqual(Quantity.of(3))).isTrue();
        assertThat(Quantity.of(2).isGreaterThanOrEqual(Quantity.of(3))).isFalse();
    }
}
