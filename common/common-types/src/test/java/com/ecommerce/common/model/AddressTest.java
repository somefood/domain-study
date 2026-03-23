package com.ecommerce.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AddressTest {

    @Test
    @DisplayName("같은 주소 정보를 가진 Address는 동일하다 (값 동등성)")
    void equals_sameValues() {
        Address a = new Address("서울", "테헤란로 1", "06130", "2층");
        Address b = new Address("서울", "테헤란로 1", "06130", "2층");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("주소 정보가 하나라도 다르면 다른 Address이다")
    void notEquals_differentValues() {
        Address a = new Address("서울", "테헤란로 1", "06130", "");
        Address b = new Address("부산", "테헤란로 1", "06130", "");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("필수 필드가 비어있으면 예외 발생")
    void constructor_emptyRequiredField_throwsException() {
        assertThatThrownBy(() -> new Address("", "테헤란로 1", "06130", ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Address("서울", "", "06130", ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Address("서울", "테헤란로 1", "", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("상세주소(detail)는 null이면 빈 문자열로 처리")
    void constructor_nullDetail_becomesEmpty() {
        Address address = new Address("서울", "테헤란로 1", "06130", null);

        assertThat(address.getDetail()).isEmpty();
    }
}
