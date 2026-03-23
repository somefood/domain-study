package com.ecommerce.order.domain.model;

import com.ecommerce.common.model.Address;
import com.ecommerce.common.model.Money;
import com.ecommerce.common.model.Quantity;
import com.ecommerce.order.domain.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    // ── 테스트 헬퍼 ──

    private static final Address SHIPPING_ADDRESS =
            new Address("서울", "테헤란로 1", "06130", "2층");

    private OrderLine createOrderLine(String skuId, long price, int qty) {
        return new OrderLine(skuId, "상품 " + skuId, Money.krw(price), Quantity.of(qty));
    }

    private Order createOrder() {
        return Order.place(
                "member-1",
                List.of(createOrderLine("sku-1", 10000, 2)),
                SHIPPING_ADDRESS
        );
    }

    /** CREATED → PAID 상태의 주문 */
    private Order createPaidOrder() {
        Order order = createOrder();
        order.pay("pay-1", Money.krw(20000));
        order.clearEvents();
        return order;
    }

    /** PAID → PREPARING 상태의 주문 */
    private Order createPreparingOrder() {
        Order order = createPaidOrder();
        order.prepare();
        order.clearEvents();
        return order;
    }

    /** PREPARING → SHIPPED 상태의 주문 */
    private Order createShippedOrder() {
        Order order = createPreparingOrder();
        order.ship("TRACK-001");
        order.clearEvents();
        return order;
    }

    // ── 주문 생성 ──

    @Nested
    @DisplayName("주문 생성")
    class Placing {

        @Test
        @DisplayName("주문을 생성하면 CREATED 상태로 시작한다")
        void place_createsWithCreatedStatus() {
            Order order = createOrder();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(order.getOrdererId()).isEqualTo("member-1");
            assertThat(order.getOrderLines()).hasSize(1);
            assertThat(order.getOrderedAt()).isNotNull();
        }

        @Test
        @DisplayName("주문 생성 시 OrderPlaced 이벤트가 발행된다")
        void place_publishesEvent() {
            Order order = createOrder();

            assertThat(order.getEvents()).hasSize(1);
            assertThat(order.getEvents().get(0)).isInstanceOf(OrderPlaced.class);

            OrderPlaced event = (OrderPlaced) order.getEvents().get(0);
            assertThat(event.getOrdererId()).isEqualTo("member-1");
            assertThat(event.getTotalAmount()).isEqualTo(Money.krw(20000)); // 10000 × 2
        }

        @Test
        @DisplayName("주문 총액은 모든 항목의 소계 합산이다")
        void place_totalAmountCalculation() {
            Order order = Order.place(
                    "member-1",
                    List.of(
                            createOrderLine("sku-1", 10000, 2),  // 20,000
                            createOrderLine("sku-2", 15000, 3)   // 45,000
                    ),
                    SHIPPING_ADDRESS
            );

            assertThat(order.getTotalAmount()).isEqualTo(Money.krw(65000));
        }

        @Test
        @DisplayName("주문 항목이 없으면 예외 발생")
        void place_emptyOrderLines_throwsException() {
            assertThatThrownBy(() -> Order.place("member-1", List.of(), SHIPPING_ADDRESS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소 1개");
        }

        @Test
        @DisplayName("배송지는 스냅샷이다 (주문 시점의 주소)")
        void place_addressIsSnapshot() {
            Order order = createOrder();

            // 주문의 배송지는 주문 생성 시점에 고정됨
            assertThat(order.getShippingAddress()).isEqualTo(SHIPPING_ADDRESS);
        }
    }

    // ── 상태 전이: 정상 플로우 ──

    @Nested
    @DisplayName("상태 전이 - 정상 플로우")
    class ValidTransitions {

        @Test
        @DisplayName("CREATED → PAID")
        void pay() {
            Order order = createOrder();
            order.pay("pay-1", Money.krw(20000));

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("PAID → PREPARING")
        void prepare() {
            Order order = createPaidOrder();
            order.prepare();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        }

        @Test
        @DisplayName("PREPARING → SHIPPED")
        void ship() {
            Order order = createPreparingOrder();
            order.ship("TRACK-001");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("SHIPPED → DELIVERED")
        void deliver() {
            Order order = createShippedOrder();
            order.deliver();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("DELIVERED → COMPLETED")
        void complete() {
            Order order = createShippedOrder();
            order.deliver();
            order.complete();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("전체 플로우: CREATED → PAID → PREPARING → SHIPPED → DELIVERED → COMPLETED")
        void fullFlow() {
            Order order = createOrder();

            order.pay("pay-1", Money.krw(20000));
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            order.prepare();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

            order.ship("TRACK-001");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);

            order.deliver();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);

            order.complete();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }
    }

    // ── 상태 전이: 잘못된 전이 ──

    @Nested
    @DisplayName("상태 전이 - 잘못된 전이")
    class InvalidTransitions {

        @Test
        @DisplayName("CREATED에서 바로 ship() 불가 (결제 안 했는데 배송?)")
        void created_cannotShip() {
            Order order = createOrder();

            assertThatThrownBy(() -> order.ship("TRACK-001"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("PAID에서 바로 deliver() 불가 (출고도 안 했는데 배송 완료?)")
        void paid_cannotDeliver() {
            Order order = createPaidOrder();

            assertThatThrownBy(order::deliver)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("이미 결제된 주문에 다시 pay() 불가")
        void paid_cannotPayAgain() {
            Order order = createPaidOrder();

            assertThatThrownBy(() -> order.pay("pay-2", Money.krw(20000)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPLETED에서 어떤 전이도 불가")
        void completed_cannotTransition() {
            Order order = createShippedOrder();
            order.deliver();
            order.complete();

            assertThatThrownBy(() -> order.cancel("변심"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── 취소 ──

    @Nested
    @DisplayName("주문 취소")
    class Cancellation {

        @Test
        @DisplayName("CREATED 상태에서 취소 가능")
        void cancel_fromCreated() {
            Order order = createOrder();

            order.cancel("변심");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancellationReason()).isEqualTo("변심");
            assertThat(order.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("PAID 상태에서 취소 가능")
        void cancel_fromPaid() {
            Order order = createPaidOrder();

            order.cancel("변심");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PREPARING 상태에서 취소 가능 (배송 전)")
        void cancel_fromPreparing() {
            Order order = createPreparingOrder();

            order.cancel("변심");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("SHIPPED 상태에서는 취소 불가 (반품으로 처리)")
        void cancel_fromShipped_throwsException() {
            Order order = createShippedOrder();

            assertThatThrownBy(() -> order.cancel("변심"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("반품");
        }

        @Test
        @DisplayName("취소 시 OrderCancelled 이벤트가 발행된다")
        void cancel_publishesEvent() {
            Order order = createOrder();
            order.clearEvents();

            order.cancel("결제 실패");

            assertThat(order.getEvents()).hasSize(1);
            OrderCancelled event = (OrderCancelled) order.getEvents().get(0);
            assertThat(event.getReason()).isEqualTo("결제 실패");
        }
    }

    // ── 가격 스냅샷 ──

    @Nested
    @DisplayName("가격 스냅샷")
    class PriceSnapshot {

        @Test
        @DisplayName("OrderLine의 가격은 주문 시점에 고정된다 (불변)")
        void orderLine_priceIsImmutable() {
            OrderLine line = createOrderLine("sku-1", 10000, 2);

            // unitPrice는 생성 시점에 고정. setter가 없으므로 변경 불가.
            assertThat(line.getUnitPrice()).isEqualTo(Money.krw(10000));
            assertThat(line.getLineTotal()).isEqualTo(Money.krw(20000));
        }
    }
}
