# Phase 2: 재고 Context (3-4주차)

## 학습할 도메인 개념

### 재고 vs 가용수량
- **실물재고(Total Stock)**: 창고에 물리적으로 있는 수량
- **예약수량(Reserved)**: 주문 처리 중 임시 보류된 수량
- **가용수량(Available)**: 실물재고 - 예약수량 = 실제 판매 가능한 수량

### 재고 예약 (Reservation)
- 고객이 주문을 넣으면 바로 재고를 차감하지 않음
- 대신 "예약"을 걸어서 다른 고객이 같은 재고를 구매하지 못하게 함
- 결제가 완료되면 예약 → 확정 (실제 차감)
- 결제 실패하면 예약 → 취소 (가용수량 복귀)

### 예약 만료
- 일정 시간(예: 30분) 내 결제가 안 되면 자동으로 예약 해제
- 이를 통해 "장바구니에 넣어놓고 안 사는" 상황에서 재고가 묶이는 것을 방지

### 카탈로그와 재고를 분리하는 이유
- **카탈로그**: "무엇을 파는가" — 상품 정보, 설명, 가격
- **재고**: "얼마나 있는가" — 수량, 위치, 예약
- 변경 빈도가 다르고, 일관성 요구도 다름
- 재고는 카탈로그의 `Product`를 모른다. `SkuId`만 알면 됨

---

## Bounded Context 정의

**책임**: 물리적 재고 수량 추적, 예약 관리, 과다판매(overselling) 방지

### 유비쿼터스 언어

| 용어 | 정의 |
|------|------|
| Stock (재고) | 특정 위치에 있는 SKU의 물리적 수량 |
| Available Quantity (가용수량) | 재고 - 예약수량 |
| Reservation (예약) | 대기 중인 주문을 위한 임시 재고 보류 |
| Stock Adjustment (재고 조정) | 재고 수량의 수동 보정 (입고, 파손, 감사) |
| SKU Reference | 카탈로그 SKU에 대한 참조 (ID만, Product 모델 X) |

---

## 핵심 Aggregate 설계

### InventoryItem (Aggregate Root)
```
InventoryItem
├── inventoryItemId: InventoryItemId (VO)
├── skuId: SkuId (VO, 카탈로그 컨텍스트 참조)
├── totalStock: Quantity (VO)
├── reservedQuantity: Quantity (VO)
├── availableQuantity(): Quantity  // 파생값: totalStock - reservedQuantity
├── reservations: List<Reservation> (Entity, InventoryItem이 소유)
│   ├── reservationId: ReservationId
│   ├── orderId: OrderId
│   ├── quantity: Quantity
│   ├── expiresAt: LocalDateTime
│   └── status: ReservationStatus (PENDING, CONFIRMED, CANCELLED, EXPIRED)
└── version: Long (낙관적 잠금)
```

---

## 도메인 이벤트

| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| StockReceived | 재고 입고 | skuId, quantity, reason |
| StockReserved | 재고 예약 | skuId, orderId, quantity, expiresAt |
| ReservationConfirmed | 예약 확정 (결제 완료) | reservationId, orderId |
| ReservationCancelled | 예약 취소 | reservationId, orderId, reason |
| ReservationExpired | 예약 만료 | reservationId, orderId |
| StockAdjusted | 재고 조정 | skuId, adjustmentType, quantity, reason |

---

## 카탈로그와의 연동

- **패턴**: Partnership — 카탈로그가 `ProductRegistered` 발행, 재고가 구독
- **구현**: Spring ApplicationEventPublisher (인프로세스, 나중에 메시징으로 교체)
- **Anti-corruption**: 재고는 카탈로그의 `Product` 클래스를 import하지 않음. `SkuId`만 알면 됨

---

## 구현할 것

1. `InventoryItem` Aggregate (예약 로직 포함)
2. 예약 / 확정 / 취소 / 만료 플로우
3. 재고 조정 (입고, 차감)
4. 낙관적 잠금 (`@Version` 필드)
5. `ProductRegistered` 이벤트 리스너 → InventoryItem 자동 생성
6. 만료 예약 정리 스케줄러 (`@Scheduled`)

## Stub/Mock
- 다중 창고 (단일 창고로 단순화)
- 공급처/발주 (수동 재고 조정으로 대체)

---

## 검증 항목

- [ ] 가용수량보다 많은 예약 시도 → 예외
- [ ] 예약 확정 → 재고 영구 차감
- [ ] 예약 취소 → 가용수량 복귀
- [ ] 만료된 예약 자동 해제
- [ ] 동시성 테스트: 마지막 1개에 2건 동시 예약 → 1건만 성공
- [ ] 카탈로그 상품 등록 → InventoryItem 자동 생성 통합 테스트
