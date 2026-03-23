package com.ecommerce.catalog.domain.event;

import com.ecommerce.common.event.DomainEvent;

/**
 * 상품이 등록되었을 때 발행되는 도메인 이벤트.
 *
 * 이 이벤트를 재고 컨텍스트가 구독하여 InventoryItem을 자동 생성한다.
 * → 카탈로그는 재고를 모른다. 이벤트만 발행할 뿐.
 */
public class ProductRegistered extends DomainEvent {

    private final String productId;
    private final String name;
    private final String sellerId;

    public ProductRegistered(String productId, String name, String sellerId) {
        this.productId = productId;
        this.name = name;
        this.sellerId = sellerId;
    }

    @Override
    public String getAggregateId() {
        return productId;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getSellerId() {
        return sellerId;
    }
}
