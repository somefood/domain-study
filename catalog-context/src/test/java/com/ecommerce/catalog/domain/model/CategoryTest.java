package com.ecommerce.catalog.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CategoryTest {

    @Test
    @DisplayName("최상위 카테고리를 생성할 수 있다")
    void createRoot() {
        Category root = Category.createRoot("전자제품");

        assertThat(root.getName()).isEqualTo("전자제품");
        assertThat(root.isRoot()).isTrue();
        assertThat(root.getDepth()).isEqualTo(0);
        assertThat(root.getParentCategoryId()).isNull();
    }

    @Test
    @DisplayName("하위 카테고리를 생성하면 부모 ID와 depth가 올바르게 설정된다")
    void createChild() {
        Category root = Category.createRoot("전자제품");

        Category child = root.createChild("스마트폰");

        assertThat(child.getName()).isEqualTo("스마트폰");
        assertThat(child.isRoot()).isFalse();
        assertThat(child.getDepth()).isEqualTo(1);
        assertThat(child.getParentCategoryId()).isEqualTo(root.getCategoryId());
    }

    @Test
    @DisplayName("카테고리 계층을 여러 단계로 만들 수 있다")
    void multiLevelHierarchy() {
        Category root = Category.createRoot("전자제품");       // depth 0
        Category mid = root.createChild("스마트폰");           // depth 1
        Category leaf = mid.createChild("안드로이드");          // depth 2

        assertThat(leaf.getDepth()).isEqualTo(2);
        assertThat(leaf.getParentCategoryId()).isEqualTo(mid.getCategoryId());
    }

    @Test
    @DisplayName("카테고리 이름은 필수이다")
    void createWithEmptyName_throwsException() {
        assertThatThrownBy(() -> Category.createRoot(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("카테고리 이름을 변경할 수 있다")
    void changeName() {
        Category category = Category.createRoot("전자제품");

        category.changeName("가전제품");

        assertThat(category.getName()).isEqualTo("가전제품");
    }
}
