package com.ecommerce.common.model;

import com.ecommerce.common.event.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 모든 Aggregate Root의 베이스 클래스.
 *
 * Aggregate Root란?
 * - Aggregate(일관성 경계) 내부 수정의 유일한 진입점
 * - 외부에서는 반드시 Root를 통해서만 내부 객체를 변경
 *
 * 이 클래스의 역할:
 * - 도메인 이벤트를 모아뒀다가 한꺼번에 발행할 수 있게 관리
 * - Aggregate에서 상태가 바뀔 때 registerEvent()를 호출하면,
 *   나중에 Application Service가 getEvents()로 꺼내서 발행한다
 *
 * 예시:
 *   order.place(orderLines);  // 내부에서 registerEvent(new OrderPlaced(...))
 *   orderRepository.save(order);
 *   eventPublisher.publishAll(order.getEvents());  // 이벤트 발행
 *   order.clearEvents();
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> events = new ArrayList<>();

    /**
     * 도메인 이벤트를 등록한다.
     * Aggregate 내부에서 상태 변경 시 호출.
     */
    protected void registerEvent(DomainEvent event) {
        events.add(event);
    }

    /**
     * 등록된 도메인 이벤트 목록을 반환한다 (읽기 전용).
     */
    public List<DomainEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * 이벤트 발행 후 목록을 비운다.
     */
    public void clearEvents() {
        events.clear();
    }
}
