package com.ecommerce.common.model;

import com.ecommerce.common.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * AggregateRoot 베이스 클래스 테스트.
 *
 * 테스트용 Aggregate와 Event를 내부 클래스로 만들어서 검증.
 */
class AggregateRootTest {

    // 테스트용 도메인 이벤트
    static class TestEvent extends DomainEvent {
        private final String aggregateId;

        TestEvent(String aggregateId) {
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }

    // 테스트용 Aggregate Root
    static class TestAggregate extends AggregateRoot {
        private final String id;

        TestAggregate(String id) {
            this.id = id;
        }

        void doSomething() {
            // 상태를 변경하고 이벤트를 등록
            registerEvent(new TestEvent(id));
        }
    }

    @Test
    @DisplayName("상태 변경 시 도메인 이벤트가 등록된다")
    void registerEvent_addsToEventList() {
        TestAggregate aggregate = new TestAggregate("agg-1");

        aggregate.doSomething();

        assertThat(aggregate.getEvents()).hasSize(1);
        assertThat(aggregate.getEvents().get(0).getAggregateId()).isEqualTo("agg-1");
    }

    @Test
    @DisplayName("여러 이벤트를 등록할 수 있다")
    void registerMultipleEvents() {
        TestAggregate aggregate = new TestAggregate("agg-1");

        aggregate.doSomething();
        aggregate.doSomething();
        aggregate.doSomething();

        assertThat(aggregate.getEvents()).hasSize(3);
    }

    @Test
    @DisplayName("이벤트 목록은 읽기 전용이다 (외부에서 조작 불가)")
    void getEvents_returnsUnmodifiableList() {
        TestAggregate aggregate = new TestAggregate("agg-1");
        aggregate.doSomething();

        assertThatThrownBy(() -> aggregate.getEvents().add(new TestEvent("hack")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("clearEvents()로 발행 완료 후 이벤트 목록을 비운다")
    void clearEvents_emptiesList() {
        TestAggregate aggregate = new TestAggregate("agg-1");
        aggregate.doSomething();
        aggregate.doSomething();

        aggregate.clearEvents();

        assertThat(aggregate.getEvents()).isEmpty();
    }

    @Test
    @DisplayName("도메인 이벤트에는 고유 ID와 발생 시각이 자동으로 부여된다")
    void domainEvent_hasIdAndTimestamp() {
        TestAggregate aggregate = new TestAggregate("agg-1");
        aggregate.doSomething();

        DomainEvent event = aggregate.getEvents().get(0);
        assertThat(event.getEventId()).isNotNull().isNotEmpty();
        assertThat(event.getOccurredAt()).isNotNull();
    }
}
