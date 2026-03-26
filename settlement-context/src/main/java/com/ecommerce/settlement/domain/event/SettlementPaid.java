package com.ecommerce.settlement.domain.event;

import com.ecommerce.common.event.DomainEvent;

import java.time.LocalDateTime;

public class SettlementPaid extends DomainEvent {

    private final String settlementId;
    private final LocalDateTime paidAt;

    public SettlementPaid(String settlementId, LocalDateTime paidAt) {
        this.settlementId = settlementId;
        this.paidAt = paidAt;
    }

    @Override
    public String getAggregateId() { return settlementId; }

    public String getSettlementId() { return settlementId; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
