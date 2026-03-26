package com.ecommerce.settlement.domain.event;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.model.Money;

public class SettlementCalculated extends DomainEvent {

    private final String settlementId;
    private final String sellerId;
    private final Money netAmount;

    public SettlementCalculated(String settlementId, String sellerId, Money netAmount) {
        this.settlementId = settlementId;
        this.sellerId = sellerId;
        this.netAmount = netAmount;
    }

    @Override
    public String getAggregateId() { return settlementId; }

    public String getSettlementId() { return settlementId; }
    public String getSellerId() { return sellerId; }
    public Money getNetAmount() { return netAmount; }
}
