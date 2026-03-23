package com.ecommerce.inventory.domain.model;

import com.ecommerce.common.model.Quantity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 재고 예약 Entity.
 *
 * Entity인 이유: 고유 ID가 있고, 상태가 변한다 (PENDING → CONFIRMED/CANCELLED/EXPIRED).
 * InventoryItem Aggregate 안에 속하므로 독립적으로 존재할 수 없다.
 *
 * 예약의 생명주기:
 * 1. 주문 생성 → 예약 생성 (PENDING)
 * 2-a. 결제 성공 → 예약 확정 (CONFIRMED) → 재고 영구 차감
 * 2-b. 결제 실패 → 예약 취소 (CANCELLED) → 가용수량 복귀
 * 2-c. 시간 초과 → 예약 만료 (EXPIRED) → 가용수량 복귀
 */
public class Reservation {

    private final String reservationId;
    private final String orderId;
    private final Quantity quantity;
    private final LocalDateTime expiresAt;
    private ReservationStatus status;

    public Reservation(String orderId, Quantity quantity, LocalDateTime expiresAt) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("주문 ID는 필수입니다");
        }
        if (quantity == null || quantity.isZero()) {
            throw new IllegalArgumentException("예약 수량은 1 이상이어야 합니다");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("만료 시각은 필수입니다");
        }
        this.reservationId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.PENDING;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 확정할 수 있습니다. 현재: " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 취소할 수 있습니다. 현재: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 만료 처리할 수 있습니다. 현재: " + status);
        }
        this.status = ReservationStatus.EXPIRED;
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public boolean isExpired(LocalDateTime now) {
        return isPending() && now.isAfter(expiresAt);
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return reservationId.equals(that.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }
}
