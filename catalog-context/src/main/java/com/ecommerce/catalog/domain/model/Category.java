package com.ecommerce.catalog.domain.model;

import com.ecommerce.common.model.AggregateRoot;

/**
 * 카테고리 Aggregate Root.
 *
 * Product와 별도의 Aggregate인 이유:
 * - 카테고리 이름을 바꿔도 상품에 영향 없어야 함
 * - 카테고리 트리 구조 변경은 상품과 독립적
 * - Product는 CategoryId로만 참조 (직접 객체 참조 X)
 */
public class Category extends AggregateRoot {

    private final CategoryId categoryId;
    private String name;
    private final CategoryId parentCategoryId; // null이면 최상위 카테고리
    private final int depth;

    public Category(String name, CategoryId parentCategoryId, int depth) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 필수입니다");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("깊이는 0 이상이어야 합니다");
        }
        this.categoryId = CategoryId.generate();
        this.name = name;
        this.parentCategoryId = parentCategoryId;
        this.depth = depth;
    }

    /**
     * 최상위 카테고리 생성 편의 메서드.
     */
    public static Category createRoot(String name) {
        return new Category(name, null, 0);
    }

    /**
     * 하위 카테고리 생성.
     */
    public Category createChild(String childName) {
        return new Category(childName, this.categoryId, this.depth + 1);
    }

    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 필수입니다");
        }
        this.name = newName;
    }

    public boolean isRoot() {
        return parentCategoryId == null;
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public CategoryId getParentCategoryId() {
        return parentCategoryId;
    }

    public int getDepth() {
        return depth;
    }
}
