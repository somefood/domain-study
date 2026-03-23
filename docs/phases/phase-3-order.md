# Phase 3: 주문 Context (5-7주차)

## 학습할 도메인 개념

### 주문은 상태 머신이다
- 주문은 단순 CRUD가 아님. 상태 전이 규칙이 있는 상태 머신
- 각 상태에서 허용되는 행동이 다름 (예: SHIPPED 상태에서는 취소 불가)
- 상태 전이마다 도메인 이벤트가 발생

### 주문 생명주기
```
  place()      ┌─────────┐   pay()    ┌────────┐  prepare()  ┌─────────┐
──────────────▶│ CREATED │──────────▶│  PAID  │───────────▶│PREPARING│
               └────┬────┘          └────────┘            └────┬────┘
                    │ cancel()                                  │ ship()
                    ▼                                           ▼
               ┌──────────┐                             ┌──────────┐
               │CANCELLED │◀─── cancel(배송 전만) ──────│ SHIPPED  │
               └──────────┘                             └────┬─────┘
                                                             │ deliver()
                                                             ▼
                                                        ┌───────────┐
                                                        │ DELIVERED │
                                                        └────┬──────┘
                                                             │ confirm()
                                                             ▼
                                                        ┌──────────┐
                                                        │COMPLETED │
                                                        └──────────┘
```

### 가격 스냅샷
- 주문은 **주문 시점의 가격**을 캡처해야 함
- 나중에 카탈로그에서 가격이 바뀌어도 주문의 가격은 변하지 않음
- 이것이 Order가 Catalog의 Product를 직접 참조하지 않는 이유

### 멱등성 (Idempotency)
- 네트워크 오류로 같은 주문 요청이 두 번 올 수 있음
- 같은 주문을 두 번 생성하면 안 됨

### Saga/Process Manager (개념 소개)
- 주문 생성 시 여러 컨텍스트가 협력해야 함:
  1. 재고 예약 → 2. 결제 처리
- 중간에 실패하면 보상 트랜잭션 필요 (예약 해제)
- 이 패턴을 **Saga** 또는 **Process Manager**라 부름
- Phase 4에서 완전히 구현

---

## Bounded Context 정의

**책임**: 고객 구매의 생명주기 관리 — 주문 생성부터 완료까지

### 유비쿼터스 언어

| 용어 | 정의 |
|------|------|
| Order (주문) | 고객의 구매 의도, 배송 완료까지 추적 |
| OrderLine (주문항목) | 주문 내 단일 상품/SKU + 수량 |
| Order Status (주문 상태) | 주문의 현재 생명주기 상태 |
| Orderer (주문자) | 주문을 넣는 고객 (회원 컨텍스트 참조) |
| Shipping Address (배송지) | 배송될 주소 (스냅샷, 참조 아님) |
| Price Snapshot (가격 스냅샷) | 주문 시점에 캡처된 가격 |

---

## 핵심 Aggregate 설계

### Order (Aggregate Root)
```
Order
├── orderId: OrderId (VO)
├── ordererId: MemberId (VO, ID로만 참조)
├── orderLines: List<OrderLine> (Entity, Order가 소유)
│   ├── orderLineId: OrderLineId
│   ├── skuId: SkuId
│   ├── productName: String (스냅샷)
│   ├── unitPrice: Money (주문 시점 스냅샷)
│   ├── quantity: Quantity
│   └── lineTotal(): Money  // unitPrice × quantity
├── shippingAddress: Address (VO, 스냅샷)
├── status: OrderStatus (전이 규칙 포함)
├── totalAmount(): Money  // 모든 lineTotal의 합
├── orderedAt: LocalDateTime
├── paidAt: LocalDateTime?
├── cancelledAt: LocalDateTime?
├── cancellationReason: String?
└── events: List<DomainEvent>
```

---

## 도메인 이벤트

| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| OrderPlaced | 주문 생성 | orderId, ordererId, orderLines, totalAmount |
| OrderPaid | 결제 완료 확인 | orderId, paymentId, paidAmount |
| OrderCancelled | 주문 취소 | orderId, reason, cancelledAt |
| OrderShipped | 출고 | orderId, trackingNumber |
| OrderDelivered | 배송 완료 | orderId, deliveredAt |
| OrderCompleted | 구매 확정 | orderId, completedAt |

---

## 크로스 컨텍스트 연동

| 연동 | 패턴 | 설명 |
|------|------|------|
| 카탈로그 → 주문 | ACL (Anti-Corruption Layer) | 주문이 상품 정보를 읽되, 자체 표현(가격 스냅샷)을 가짐 |
| 주문 → 재고 | 동기 호출 (같은 프로세스) | 주문 생성 시 재고 예약 |
| 주문 → 결제 | 도메인 이벤트 | `OrderPlaced` 발행 → 결제가 구독 (Phase 4) |

---

## 구현할 것

1. `Order` Aggregate (상태 머신 강제)
2. 상태 전이 검증 (예: CREATED → SHIPPED 불가)
3. `OrderService` 도메인 서비스 (주문 생성 오케스트레이션)
   - 상품 존재 검증 (카탈로그 ACL)
   - 재고 예약 (재고 호출)
   - 가격 스냅샷
4. `PlaceOrderUseCase`, `CancelOrderUseCase`
5. 이벤트 핸들러:
   - `PaymentCompleted` → 주문 PAID 전이
   - `PaymentFailed` → 주문 취소 + 재고 예약 해제
6. REST API

## Stub/Mock
- **결제**: 즉시 성공/실패 스텁 (Phase 4에서 구현)
- **회원**: 아무 memberId 허용 (Phase 7에서 구현)
- **장바구니**: 없음 — 주문항목을 직접 전달 (Phase 8에서 추가)

---

## 검증 항목

- [ ] 모든 유효 상태 전이가 동작
- [ ] 모든 무효 상태 전이에서 `IllegalStateTransitionException` 발생
- [ ] 배송 후 취소 불가
- [ ] 가격 스냅샷은 카탈로그 가격 변경에 영향받지 않음
- [ ] 주문 → 재고 예약 → 취소 → 재고 해제 통합 테스트
- [ ] 주문 생성 멱등성 테스트
