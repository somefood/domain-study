package com.ecommerce.common.model;

import java.util.Objects;

/**
 * 주소를 표현하는 Value Object.
 *
 * 왜 String이 아니라 별도 클래스로 만드는가?
 * - 주소에는 구조가 있다 (도시, 거리, 우편번호)
 * - 주소 관련 검증 로직을 이 클래스에 모을 수 있다
 * - "주소"라는 도메인 개념을 코드에서 명확히 표현
 *
 * Value Object이므로:
 * - 불변(immutable): 주소를 바꾸려면 새 Address를 만든다
 * - 값으로 비교: 같은 도시, 거리, 우편번호면 같은 주소
 */
public class Address {

    private final String city;
    private final String street;
    private final String zipCode;
    private final String detail;

    public Address(String city, String street, String zipCode, String detail) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("도시는 필수입니다");
        }
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("거리는 필수입니다");
        }
        if (zipCode == null || zipCode.isBlank()) {
            throw new IllegalArgumentException("우편번호는 필수입니다");
        }
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
        this.detail = detail != null ? detail : "";
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return city.equals(address.city)
                && street.equals(address.street)
                && zipCode.equals(address.zipCode)
                && detail.equals(address.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, street, zipCode, detail);
    }

    @Override
    public String toString() {
        String base = String.format("[%s] %s %s", zipCode, city, street);
        return detail.isEmpty() ? base : base + " " + detail;
    }
}
