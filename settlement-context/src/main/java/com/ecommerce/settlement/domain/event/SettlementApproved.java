package com.ecommerce.settlement.domain.event;

import com.ecommerce.common.event.DomainEvent;

public class SettlementApproved extends DomainEvent {

    private final String settlementId;

    public SettlementApproved(String settlementId) {
        this.settlementId = settlementId;
    }

    @Override
    public String getAggregateId() { return settlementId; }

    public String getSettlementId() { return settlementId; }
}
