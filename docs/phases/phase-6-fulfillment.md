# Phase 6: 배송 Context (10-12주차)

## 학습할 도메인 개념

### 배송은 단순 발송이 아니다
- 결제 완료 후의 전체 물리적 프로세스를 관리
- 접수 → 피킹(상품 꺼내기) → 포장 → 출고 → 배송중 → 배송완료

### 분할 배송 (Split Shipment)
- 하나의 주문이 여러 택배로 나뉠 수 있음
  - 다른 창고에 있는 상품
  - 크기가 달라 별도 포장이 필요한 상품
- 주문은 **모든** Shipment가 배송완료 되어야 DELIVERED

### 반품/교환 (역물류)
- 고객이 상품을 돌려보내는 플로우
- 반품 접수 → 반품 수거 → 검수 → 환불 트리거
- 교환은 "반품 + 재주문"으로 처리하는 경우가 많음

### 배송 추적
- 택배사별 운송장 번호(Tracking Number)
- 실시간 배송 상태 업데이트

---

## Bounded Context 정의

**책임**: 결제 완료된 주문의 물리적 배송 관리

### 유비쿼터스 언어

| 용어 | 정의 |
|------|------|
| Fulfillment (출고/배송) | 주문의 물리적 이행 프로세스 |
| Shipment (배송건) | 고객에게 보내는 물리적 택배 1건 |
| Picking (피킹) | 창고에서 상품을 꺼내는 행위 |
| Tracking Number (운송장 번호) | 택배사의 배송 추적 식별자 |
| Return (반품) | 고객이 상품을 돌려보내는 것 |

---

## 핵심 Aggregate 설계

### Fulfillment (Aggregate Root)
```
Fulfillment
├── fulfillmentId: FulfillmentId (VO)
├── orderId: OrderId (VO)
├── shipments: List<Shipment> (Entity, Fulfillment이 소유)
│   ├── shipmentId: ShipmentId
│   ├── items: List<ShipmentItem>
│   │   └── skuId, quantity, productName
│   ├── trackingNumber: String?
│   ├── carrier: String? (택배사)
│   ├── status: ShipmentStatus (PENDING, PICKING, PACKED, SHIPPED, DELIVERED)
│   ├── shippedAt: LocalDateTime?
│   └── deliveredAt: LocalDateTime?
├── status: FulfillmentStatus (ACCEPTED, IN_PROGRESS, COMPLETED)
└── events: List<DomainEvent>
```

---

## 도메인 이벤트

| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| FulfillmentAccepted | 배송 접수 | fulfillmentId, orderId |
| ShipmentDispatched | 출고 | fulfillmentId, shipmentId, trackingNumber |
| ShipmentDelivered | 배송 완료 | fulfillmentId, shipmentId, deliveredAt |
| ReturnRequested | 반품 요청 | fulfillmentId, orderId, items, reason |
| ReturnReceived | 반품 수령 | fulfillmentId, returnId |

---

## 반품-환불 Saga (크로스 컨텍스트)

```
고객 반품 요청
  → [배송] ReturnRequested → ReturnReceived
    → [결제] RefundRequested → RefundCompleted
      → [정산] 다음 정산에서 환불 금액 차감
        → [재고] 반품 재고 복귀 (StockReceived)
          → [주문] 주문 상태 업데이트
```

---

## 구현할 것

1. `Fulfillment` Aggregate (배송 상태 관리)
2. `Shipment` Entity (배송 추적)
3. 분할 배송 지원
4. `OrderPaid` 이벤트 리스너 → Fulfillment 자동 생성
5. 배송 상태 머신
6. 반품 플로우 (ReturnRequested → ReturnReceived → 환불 트리거)
7. 배송→주문 상태 연동 (`ShipmentDispatched` → Order SHIPPED)

## Stub/Mock
- 실제 택배사 API (가짜 어댑터)
- 창고관리 시스템
- 배송 일정 관리

---

## 검증 항목

- [ ] 배송 상태 전이 테스트
- [ ] 분할 배송: 모든 Shipment 완료 시에만 Fulfillment COMPLETED
- [ ] 주문 결제 → 배송 생성 → 출고 → 배송완료 → 주문 완료 통합 테스트
- [ ] 반품 E2E (반품 요청 → 수령 → 환불 → 정산 차감) 테스트
