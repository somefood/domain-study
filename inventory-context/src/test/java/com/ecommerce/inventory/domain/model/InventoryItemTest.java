package com.ecommerce.inventory.domain.model;

import com.ecommerce.common.model.Quantity;
import com.ecommerce.inventory.domain.event.ReservationCancelled;
import com.ecommerce.inventory.domain.event.ReservationConfirmed;
import com.ecommerce.inventory.domain.event.StockReceived;
import com.ecommerce.inventory.domain.event.StockReserved;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class InventoryItemTest {

    private static final LocalDateTime EXPIRES_IN_30_MIN = LocalDateTime.now().plusMinutes(30);

    private InventoryItem createItemWithStock(int stock) {
        InventoryItem item = InventoryItem.create("sku-001");
        item.receiveStock(Quantity.of(stock), "초기 입고");
        item.clearEvents();
        return item;
    }

    // ── 입고 ──

    @Nested
    @DisplayName("재고 입고")
    class StockReceiving {

        @Test
        @DisplayName("입고하면 실물재고와 가용수량이 증가한다")
        void receiveStock_increasesQuantity() {
            InventoryItem item = InventoryItem.create("sku-001");

            item.receiveStock(Quantity.of(100), "신규 입고");

            assertThat(item.getTotalStock()).isEqualTo(Quantity.of(100));
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(100));
        }

        @Test
        @DisplayName("여러 번 입고하면 재고가 누적된다")
        void receiveStock_accumulates() {
            InventoryItem item = InventoryItem.create("sku-001");

            item.receiveStock(Quantity.of(50), "1차 입고");
            item.receiveStock(Quantity.of(30), "2차 입고");

            assertThat(item.getTotalStock()).isEqualTo(Quantity.of(80));
        }

        @Test
        @DisplayName("입고 시 StockReceived 이벤트가 발행된다")
        void receiveStock_publishesEvent() {
            InventoryItem item = InventoryItem.create("sku-001");

            item.receiveStock(Quantity.of(100), "입고");

            assertThat(item.getEvents()).hasSize(1);
            assertThat(item.getEvents().get(0)).isInstanceOf(StockReceived.class);
        }

        @Test
        @DisplayName("입고 수량이 0이면 예외 발생")
        void receiveStock_zeroQuantity_throwsException() {
            InventoryItem item = InventoryItem.create("sku-001");

            assertThatThrownBy(() -> item.receiveStock(Quantity.zero(), "오류"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── 예약 ──

    @Nested
    @DisplayName("재고 예약")
    class Reserving {

        @Test
        @DisplayName("가용수량 내에서 예약할 수 있다")
        void reserve_withinAvailable_succeeds() {
            InventoryItem item = createItemWithStock(10);

            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);

            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(7));
            assertThat(item.getReservedQuantity()).isEqualTo(Quantity.of(3));
            assertThat(item.getTotalStock()).isEqualTo(Quantity.of(10)); // 실물재고는 변하지 않음
        }

        @Test
        @DisplayName("여러 주문에 대해 예약할 수 있다")
        void reserve_multipleOrders() {
            InventoryItem item = createItemWithStock(10);

            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);
            item.reserve("order-2", Quantity.of(4), EXPIRES_IN_30_MIN);

            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(3));
            assertThat(item.getReservedQuantity()).isEqualTo(Quantity.of(7));
        }

        @Test
        @DisplayName("가용수량보다 많이 예약하면 예외 발생 (과다판매 방지)")
        void reserve_exceedsAvailable_throwsException() {
            InventoryItem item = createItemWithStock(5);

            assertThatThrownBy(() -> item.reserve("order-1", Quantity.of(6), EXPIRES_IN_30_MIN))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("가용 재고가 부족합니다");
        }

        @Test
        @DisplayName("이미 예약으로 가용수량이 줄어든 상태에서 초과 예약 불가")
        void reserve_afterPartialReservation_exceedsAvailable() {
            InventoryItem item = createItemWithStock(5);
            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);

            // 가용 2개인데 3개 예약 시도
            assertThatThrownBy(() -> item.reserve("order-2", Quantity.of(3), EXPIRES_IN_30_MIN))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("예약 시 StockReserved 이벤트가 발행된다")
        void reserve_publishesEvent() {
            InventoryItem item = createItemWithStock(10);

            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);

            assertThat(item.getEvents()).hasSize(1);
            assertThat(item.getEvents().get(0)).isInstanceOf(StockReserved.class);
        }
    }

    // ── 예약 확정 ──

    @Nested
    @DisplayName("예약 확정")
    class Confirming {

        @Test
        @DisplayName("예약 확정 시 실물재고가 영구 차감된다")
        void confirm_reducesTotalStock() {
            InventoryItem item = createItemWithStock(10);
            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);

            item.confirmReservation("order-1");

            assertThat(item.getTotalStock()).isEqualTo(Quantity.of(7));    // 10 - 3
            assertThat(item.getReservedQuantity()).isEqualTo(Quantity.of(0)); // 확정 후 PENDING 없음
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(7));
        }

        @Test
        @DisplayName("확정 시 ReservationConfirmed 이벤트가 발행된다")
        void confirm_publishesEvent() {
            InventoryItem item = createItemWithStock(10);
            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);
            item.clearEvents();

            item.confirmReservation("order-1");

            assertThat(item.getEvents()).hasSize(1);
            assertThat(item.getEvents().get(0)).isInstanceOf(ReservationConfirmed.class);
        }

        @Test
        @DisplayName("존재하지 않는 주문의 예약을 확정하면 예외 발생")
        void confirm_unknownOrder_throwsException() {
            InventoryItem item = createItemWithStock(10);

            assertThatThrownBy(() -> item.confirmReservation("unknown-order"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }
    }

    // ── 예약 취소 ──

    @Nested
    @DisplayName("예약 취소")
    class Cancelling {

        @Test
        @DisplayName("예약 취소 시 가용수량이 복귀된다")
        void cancel_restoresAvailableQuantity() {
            InventoryItem item = createItemWithStock(10);
            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);

            item.cancelReservation("order-1", "결제 실패");

            assertThat(item.getTotalStock()).isEqualTo(Quantity.of(10));     // 실물재고 변화 없음
            assertThat(item.getReservedQuantity()).isEqualTo(Quantity.of(0));
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(10)); // 가용수량 복귀
        }

        @Test
        @DisplayName("취소 시 ReservationCancelled 이벤트가 발행된다")
        void cancel_publishesEvent() {
            InventoryItem item = createItemWithStock(10);
            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);
            item.clearEvents();

            item.cancelReservation("order-1", "주문 취소");

            assertThat(item.getEvents()).hasSize(1);
            assertThat(item.getEvents().get(0)).isInstanceOf(ReservationCancelled.class);
        }
    }

    // ── 예약 만료 ──

    @Nested
    @DisplayName("예약 만료")
    class Expiring {

        @Test
        @DisplayName("만료 시각이 지난 예약은 자동으로 만료 처리된다")
        void expire_pastDeadline_expiresReservation() {
            InventoryItem item = createItemWithStock(10);
            LocalDateTime expiresAt = LocalDateTime.now().minusMinutes(1); // 이미 만료됨
            item.reserve("order-1", Quantity.of(3), expiresAt);

            int expiredCount = item.expireReservations(LocalDateTime.now());

            assertThat(expiredCount).isEqualTo(1);
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(10)); // 가용수량 복귀
        }

        @Test
        @DisplayName("아직 만료되지 않은 예약은 유지된다")
        void expire_notYetExpired_keepsReservation() {
            InventoryItem item = createItemWithStock(10);
            item.reserve("order-1", Quantity.of(3), EXPIRES_IN_30_MIN);

            int expiredCount = item.expireReservations(LocalDateTime.now());

            assertThat(expiredCount).isEqualTo(0);
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(7)); // 예약 유지
        }
    }

    // ── 전체 플로우 ──

    @Nested
    @DisplayName("전체 플로우")
    class FullFlow {

        @Test
        @DisplayName("입고 → 예약 → 확정 전체 플로우")
        void fullFlow_receiveReserveConfirm() {
            InventoryItem item = InventoryItem.create("sku-001");

            // 1. 입고 100개
            item.receiveStock(Quantity.of(100), "입고");
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(100));

            // 2. 주문 A: 30개 예약
            item.reserve("order-A", Quantity.of(30), EXPIRES_IN_30_MIN);
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(70));

            // 3. 주문 B: 20개 예약
            item.reserve("order-B", Quantity.of(20), EXPIRES_IN_30_MIN);
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(50));

            // 4. 주문 A 결제 성공 → 확정
            item.confirmReservation("order-A");
            assertThat(item.getTotalStock()).isEqualTo(Quantity.of(70));  // 100 - 30
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(50)); // 70 - 20(B 예약)

            // 5. 주문 B 결제 실패 → 취소
            item.cancelReservation("order-B", "결제 실패");
            assertThat(item.getTotalStock()).isEqualTo(Quantity.of(70));
            assertThat(item.getAvailableQuantity()).isEqualTo(Quantity.of(70)); // B 예약 복귀
        }
    }
}
