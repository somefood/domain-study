package com.ecommerce.common.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 돈을 표현하는 Value Object.
 *
 * 왜 int나 long이 아니라 별도 클래스로 만드는가?
 * 1. 금액에는 항상 통화(currency)가 따라다닌다 — 1000원과 1000달러는 다르다
 * 2. 금액 연산에는 규칙이 있다 — 다른 통화끼리 더하면 안 된다
 * 3. 소수점 처리가 필요하다 — float/double은 정밀도 문제가 있어 BigDecimal 사용
 *
 * Value Object의 특징:
 * - 불변(immutable): 한번 만들면 값을 바꿀 수 없다. add()는 새 객체를 반환.
 * - 값으로 비교: Money(1000, KRW) == Money(1000, KRW). ID가 아니라 값이 같으면 같은 객체.
 */
public class Money {

    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다");
        }
        if (currency == null) {
            throw new IllegalArgumentException("통화는 null일 수 없습니다");
        }
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        this.currency = currency;
    }

    /**
     * 편의 팩토리 메서드: 원화(KRW) 생성
     */
    public static Money krw(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("KRW"));
    }

    /**
     * 편의 팩토리 메서드: 임의 통화 생성
     */
    public static Money of(long amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format("통화가 다릅니다: %s vs %s", this.currency, other.currency)
            );
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    // Value Object는 값으로 비교한다
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
