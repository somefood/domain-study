package com.ecommerce.inventory.domain.model;

import com.ecommerce.common.model.AggregateRoot;
import com.ecommerce.common.model.Quantity;
import com.ecommerce.inventory.domain.event.ReservationCancelled;
import com.ecommerce.inventory.domain.event.ReservationConfirmed;
import com.ecommerce.inventory.domain.event.StockReceived;
import com.ecommerce.inventory.domain.event.StockReserved;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 재고 아이템 Aggregate Root.
 *
// * 카탈로그의 SKU 1개에 대응하는 재고 관리 단위.
 * 카탈로그의 Product나 SKU 클래스를 import하지 않는다 → skuId(String)만 알면 됨.
 * 이것이 Bounded Context 간 분리의 핵심.
 *
 * 핵심 불변식:
 * 1. 가용수량 = 실물재고 - 예약수량 (PENDING 상태만 합산)
 * 2. 가용수량보다 많이 예약할 수 없다
 * 3. 예약 확정 시 실물재고가 줄어든다 (영구 차감)
 * 4. 예약 취소/만료 시 예약수량이 줄어든다 (가용수량 복귀)
 *
 * version 필드: 낙관적 잠금용. 동시에 두 요청이 같은 재고를 변경하려 하면
 * 하나는 version 불일치로 실패한다 → 과다판매 방지.
 */
public class InventoryItem extends AggregateRoot {

    private final InventoryItemId inventoryItemId;
    private final String skuId;
    private Quantity totalStock;
    private final List<Reservation> reservations;
    private long version; // 낙관적 잠금

    /**
     * 재고 아이템 생성.
     * 카탈로그에서 ProductRegistered 이벤트를 받으면 자동으로 생성됨.
     */
    public static InventoryItem create(String skuId) {
        if (skuId == null || skuId.isBlank()) {
            throw new IllegalArgumentException("SKU ID는 필수입니다");
        }
        return new InventoryItem(InventoryItemId.generate(), skuId);
    }

    private InventoryItem(InventoryItemId inventoryItemId, String skuId) {
        this.inventoryItemId = inventoryItemId;
        this.skuId = skuId;
        this.totalStock = Quantity.zero();
        this.reservations = new ArrayList<>();
        this.version = 0;
    }

    // ── 재고 입고 ──

    /**
     * 재고 입고.
     * 실물재고가 늘어나고, 그만큼 가용수량도 늘어난다.
     */
    public void receiveStock(Quantity quantity, String reason) {
        if (quantity == null || quantity.isZero()) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다");
        }
        this.totalStock = this.totalStock.add(quantity);
        registerEvent(new StockReceived(
                inventoryItemId.getId(), skuId, quantity.getValue(), reason
        ));
    }

    // ── 예약 ──

    /**
     * 재고 예약.
     *
     * 주문이 들어오면 가용수량에서 예약을 건다.
     * 가용수량보다 많이 예약하려 하면 예외 → 과다판매 방지.
     */
    public Reservation reserve(String orderId, Quantity quantity, LocalDateTime expiresAt) {
        Quantity available = getAvailableQuantity();
        if (!available.isGreaterThanOrEqual(quantity)) {
            throw new IllegalStateException(
                    String.format("가용 재고가 부족합니다. 가용: %s, 요청: %s", available, quantity)
            );
        }

        Reservation reservation = new Reservation(orderId, quantity, expiresAt);
        reservations.add(reservation);

        registerEvent(new StockReserved(
                inventoryItemId.getId(), skuId, orderId,
                quantity.getValue(), expiresAt
        ));

        return reservation;
    }

    /**
     * 예약 확정 (결제 성공 시).
     *
     * 예약수량만큼 실물재고를 영구적으로 차감한다.
     * 예약은 CONFIRMED 상태로 바뀌고, 더 이상 가용수량 계산에 포함되지 않는다.
     */
    public void confirmReservation(String orderId) {
        Reservation reservation = findPendingReservation(orderId);
        reservation.confirm();
        this.totalStock = this.totalStock.subtract(reservation.getQuantity());

        registerEvent(new ReservationConfirmed(
                inventoryItemId.getId(), reservation.getReservationId(), orderId
        ));
    }

    /**
     * 예약 취소 (결제 실패 또는 주문 취소 시).
     *
     * 예약이 CANCELLED로 바뀌면 가용수량이 자동 복귀된다.
     * (가용수량 = 실물재고 - PENDING 예약수량이므로, PENDING이 줄면 가용이 늘어남)
     */
    public void cancelReservation(String orderId, String reason) {
        Reservation reservation = findPendingReservation(orderId);
        reservation.cancel();

        registerEvent(new ReservationCancelled(
                inventoryItemId.getId(), reservation.getReservationId(), orderId, reason
        ));
    }

    /**
     * 만료된 예약을 정리한다.
     * 스케줄러가 주기적으로 호출.
     *
     * @return 만료 처리된 예약 수
     */
    public int expireReservations(LocalDateTime now) {
        int expiredCount = 0;
        for (Reservation reservation : reservations) {
            if (reservation.isExpired(now)) {
                reservation.expire();
                expiredCount++;
            }
        }
        return expiredCount;
    }

    // ── 조회 ──

    /**
     * 가용수량 계산.
     * 실물재고에서 PENDING 상태의 예약수량을 뺀 값.
     */
    public Quantity getAvailableQuantity() {
        Quantity reserved = reservations.stream()
                .filter(Reservation::isPending)
                .map(Reservation::getQuantity)
                .reduce(Quantity.zero(), Quantity::add);
        return totalStock.subtract(reserved);
    }

    /**
     * 현재 PENDING 상태의 총 예약수량.
     */
    public Quantity getReservedQuantity() {
        return reservations.stream()
                .filter(Reservation::isPending)
                .map(Reservation::getQuantity)
                .reduce(Quantity.zero(), Quantity::add);
    }

    private Reservation findPendingReservation(String orderId) {
        return reservations.stream()
                .filter(r -> r.getOrderId().equals(orderId) && r.isPending())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 주문의 대기중인 예약을 찾을 수 없습니다: " + orderId
                ));
    }

    // Getters
    public InventoryItemId getInventoryItemId() { return inventoryItemId; }
    public String getSkuId() { return skuId; }
    public Quantity getTotalStock() { return totalStock; }
    public List<Reservation> getReservations() { return Collections.unmodifiableList(reservations); }
    public long getVersion() { return version; }
}
