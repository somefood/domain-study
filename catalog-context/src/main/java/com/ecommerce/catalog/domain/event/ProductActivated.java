package com.ecommerce.catalog.domain.event;

import com.ecommerce.common.event.DomainEvent;

import java.util.List;

public class ProductActivated extends DomainEvent {

    private final String productId;
    private final List<String> skuIds;

    public ProductActivated(String productId, List<String> skuIds) {
        this.productId = productId;
        this.skuIds = List.copyOf(skuIds);
    }

    @Override
    public String getAggregateId() {
        return productId;
    }

    public String getProductId() {
        return productId;
    }

    public List<String> getSkuIds() {
        return skuIds;
    }
}
