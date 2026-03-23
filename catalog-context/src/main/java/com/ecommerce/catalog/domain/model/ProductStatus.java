package com.ecommerce.catalog.domain.model;

/**
 * 상품 상태.
 *
 * 상태 전이 규칙:
 * - DRAFT → ACTIVE: 활성화 (SKU가 최소 1개 필요)
 * - ACTIVE → DISCONTINUED: 단종
 * - DRAFT → DISCONTINUED: 불가 (판매한 적 없는 상품은 단종이 아님)
 */
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    DISCONTINUED
}
