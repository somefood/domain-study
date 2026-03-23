package com.ecommerce.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Money Value Object 테스트.
 *
 * Value Object의 핵심 특성을 검증한다:
 * 1. 값 동등성: 같은 금액+통화면 같은 객체
 * 2. 불변성: 연산하면 새 객체가 반환되고, 원본은 변하지 않음
 * 3. 도메인 규칙: 다른 통화끼리 연산 불가
 */
class MoneyTest {

    @Test
    @DisplayName("같은 금액과 통화를 가진 Money는 동일하다 (값 동등성)")
    void equals_sameAmountAndCurrency() {
        Money a = Money.krw(1000);
        Money b = Money.krw(1000);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("금액이 다르면 다른 Money이다")
    void notEquals_differentAmount() {
        Money a = Money.krw(1000);
        Money b = Money.krw(2000);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("통화가 다르면 다른 Money이다")
    void notEquals_differentCurrency() {
        Money krw = Money.of(1000, "KRW");
        Money usd = Money.of(1000, "USD");

        assertThat(krw).isNotEqualTo(usd);
    }

    @Test
    @DisplayName("더하기: 새 객체가 반환되고 원본은 변하지 않는다 (불변성)")
    void add_returnsNewInstance() {
        Money a = Money.krw(1000);
        Money b = Money.krw(2000);

        Money result = a.add(b);

        assertThat(result).isEqualTo(Money.krw(3000));
        // 원본은 변하지 않음
        assertThat(a).isEqualTo(Money.krw(1000));
        assertThat(b).isEqualTo(Money.krw(2000));
    }

    @Test
    @DisplayName("빼기 연산")
    void subtract() {
        Money result = Money.krw(5000).subtract(Money.krw(2000));

        assertThat(result).isEqualTo(Money.krw(3000));
    }

    @Test
    @DisplayName("곱하기 연산 (수량에 단가를 곱할 때 사용)")
    void multiply() {
        Money unitPrice = Money.krw(15000);

        Money total = unitPrice.multiply(3);

        assertThat(total).isEqualTo(Money.krw(45000));
    }

    @Test
    @DisplayName("다른 통화끼리 더하면 예외 발생")
    void add_differentCurrency_throwsException() {
        Money krw = Money.krw(1000);
        Money usd = Money.of(1000, "USD");

        assertThatThrownBy(() -> krw.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("통화가 다릅니다");
    }

    @Test
    @DisplayName("양수/음수 판별")
    void isPositive_isNegative() {
        assertThat(Money.krw(1000).isPositive()).isTrue();
        assertThat(Money.krw(0).isPositive()).isFalse();
        assertThat(Money.krw(-1000).isNegative()).isTrue();
    }

    @Test
    @DisplayName("금액 비교")
    void isGreaterThanOrEqual() {
        assertThat(Money.krw(2000).isGreaterThanOrEqual(Money.krw(1000))).isTrue();
        assertThat(Money.krw(1000).isGreaterThanOrEqual(Money.krw(1000))).isTrue();
        assertThat(Money.krw(500).isGreaterThanOrEqual(Money.krw(1000))).isFalse();
    }

    @Test
    @DisplayName("null 금액으로 생성하면 예외 발생")
    void constructor_nullAmount_throwsException() {
        assertThatThrownBy(() -> new Money(null, java.util.Currency.getInstance("KRW")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
