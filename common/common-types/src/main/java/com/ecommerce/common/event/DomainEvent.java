package com.ecommerce.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 모든 도메인 이벤트의 베이스 클래스.
 *
 * 도메인 이벤트란?
 * - "도메인에서 의미 있는 일이 발생했다"를 표현하는 객체
 * - 과거형으로 이름 짓는다: OrderPlaced, PaymentCompleted, StockReserved
 * - 불변(immutable)이다 — 이미 발생한 사실은 바뀌지 않으니까
 *
 * 왜 필요한가?
 * - Bounded Context 간 느슨한 결합: Order가 Payment를 직접 호출하지 않고,
 *   OrderPlaced 이벤트를 발행하면 Payment가 구독
 * - 감사/이력 추적: 언제 무슨 일이 일어났는지 기록
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    /**
     * 이 이벤트가 발생한 Aggregate의 ID.
     * 하위 클래스에서 구현한다.
     */
    public abstract String getAggregateId();
}
