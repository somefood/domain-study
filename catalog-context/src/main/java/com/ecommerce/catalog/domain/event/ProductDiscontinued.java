package com.ecommerce.catalog.domain.event;

import com.ecommerce.common.event.DomainEvent;

public class ProductDiscontinued extends DomainEvent {

    private final String productId;

    public ProductDiscontinued(String productId) {
        this.productId = productId;
    }

    @Override
    public String getAggregateId() {
        return productId;
    }

    public String getProductId() {
        return productId;
    }
}
