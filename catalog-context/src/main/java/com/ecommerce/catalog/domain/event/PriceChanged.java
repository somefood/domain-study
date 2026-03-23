package com.ecommerce.catalog.domain.event;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.model.Money;

public class PriceChanged extends DomainEvent {

    private final String productId;
    private final String skuId;
    private final Money oldPrice;
    private final Money newPrice;

    public PriceChanged(String productId, String skuId, Money oldPrice, Money newPrice) {
        this.productId = productId;
        this.skuId = skuId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
    }

    @Override
    public String getAggregateId() {
        return productId;
    }

    public String getProductId() {
        return productId;
    }

    public String getSkuId() {
        return skuId;
    }

    public Money getOldPrice() {
        return oldPrice;
    }

    public Money getNewPrice() {
        return newPrice;
    }
}
