# Phase 4: 결제 Context (7-9주차)

## 학습할 도메인 개념

### 결제는 "결제 후 끝"이 아니다
- 결제에도 자체적인 복잡한 생명주기가 있음
- 시작, 인증, 매입, 환불 등 여러 단계를 거침

### 결제 수단별 플로우 차이
- **카드**: 인증(Authorization) → 매입(Capture) 2단계
- **계좌이체**: 즉시 출금
- **가상계좌**: 발급 → 입금 대기 → 입금 확인
- **모바일페이**: 앱 리다이렉트 → 승인

### 인증(Authorization) vs 매입(Capture)
- **인증**: 카드 한도에서 금액을 "점유"만 함. 아직 실제 청구는 안 됨
- **매입**: 점유한 금액을 실제로 청구. 이때 셀러에게 돈이 감
- 이 분리가 있어야 "주문 취소 시 인증만 해제" 같은 처리가 가능

### 환불은 "결제 취소"가 아니다
- 환불은 별도의 금융 트랜잭션
- 부분 환불이 가능 (전체 금액 중 일부만)
- 환불에도 자체 상태가 있음

### 멱등성 키 (Idempotency Key)
- 네트워크 장애로 결제 요청이 중복될 수 있음
- 같은 멱등성 키로 두 번 요청 → 두 번째는 첫 번째 결과를 반환
- 이중 결제를 원천 방지

---

## Bounded Context 정의

**책임**: 결제 처리, 결제 생명주기 관리, 환불 처리

### 유비쿼터스 언어

| 용어 | 정의 |
|------|------|
| Payment (결제) | 주문과 연결된 금융 트랜잭션 |
| Payment Method (결제 수단) | 사용된 결제 도구 (카드, 계좌이체 등) |
| Authorization (인증) | 자금 점유 (청구 전 단계) |
| Capture (매입) | 점유된 자금의 실제 청구 |
| Refund (환불) | 고객에게 돈을 돌려주는 행위 |
| PG Transaction | 외부 결제 대행사의 트랜잭션 기록 |
| Idempotency Key (멱등성 키) | 중복 결제 방지를 위한 고유 키 |

---

## 결제 상태 머신

```
┌──────────┐  authorize()  ┌────────────┐  capture()  ┌──────────┐
│ INITIATED│─────────────▶│ AUTHORIZED │───────────▶│ CAPTURED │
└────┬─────┘              └─────┬──────┘            └────┬─────┘
     │ fail()                   │ cancel()               │ refund()
     ▼                          ▼                         ▼
┌──────────┐            ┌───────────┐            ┌───────────────┐
│  FAILED  │            │ CANCELLED │            │  부분/전체     │
└──────────┘            └───────────┘            │    환불됨      │
                                                 └───────────────┘
```

---

## 핵심 Aggregate 설계

### Payment (Aggregate Root)
```
Payment
├── paymentId: PaymentId (VO)
├── orderId: OrderId (VO, ID로만 참조)
├── amount: Money (VO)
├── method: PaymentMethod (VO: CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT)
├── status: PaymentStatus
├── pgTransactionId: String? (외부 PG에서 받은 ID)
├── idempotencyKey: IdempotencyKey (VO)
├── refunds: List<Refund> (Entity, Payment가 소유)
│   ├── refundId: RefundId
│   ├── amount: Money
│   ├── reason: String
│   ├── status: RefundStatus
│   └── refundedAt: LocalDateTime
├── initiatedAt, authorizedAt, capturedAt
└── events: List<DomainEvent>
```

---

## 도메인 이벤트

| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| PaymentInitiated | 결제 시작 | paymentId, orderId, amount, method |
| PaymentAuthorized | 인증 완료 | paymentId, pgTransactionId |
| PaymentCompleted | 매입 완료 | paymentId, orderId, amount, capturedAt |
| PaymentFailed | 결제 실패 | paymentId, orderId, reason |
| PaymentCancelled | 결제 취소 | paymentId, orderId |
| RefundRequested | 환불 요청 | paymentId, refundId, amount, reason |
| RefundCompleted | 환불 완료 | paymentId, refundId, refundedAmount |

---

## 주문-결제 Saga (Process Manager)

이 Phase의 핵심. 여러 컨텍스트를 걸쳐 트랜잭션을 조율하는 패턴.

```
OrderSaga (Application Service / Process Manager):

├── OrderPlaced 수신:
│   1. 재고 예약 시도
│   2. 예약 실패 → OrderCancelled 발행 (사유: 재고 부족)
│   3. 예약 성공 → 결제 시작 (PaymentInitiated)
│
├── PaymentCompleted 수신:
│   1. 재고 예약 확정 (ReservationConfirmed)
│   2. 주문 상태 → PAID
│
├── PaymentFailed 수신:
│   1. 재고 예약 취소 (ReservationCancelled)
│   2. 주문 상태 → CANCELLED (사유: 결제 실패)
```

### 왜 Saga가 필요한가?
- DB 트랜잭션으로는 여러 Bounded Context를 묶을 수 없음
- 각 Context는 자기만의 트랜잭션 경계를 가짐
- 실패 시 "보상 트랜잭션"으로 이전 단계를 되돌림

---

## 구현할 것

1. `Payment` Aggregate (상태 머신)
2. `Refund` Entity (부분/전체 환불)
3. `PaymentGateway` 포트 (인터페이스)
4. 가짜 PG 어댑터 (성공/실패/타임아웃 시뮬레이션)
5. `InitiatePaymentUseCase`, `ConfirmPaymentUseCase`, `RequestRefundUseCase`
6. 주문-결제 Saga 구현
7. 결제→주문 이벤트 핸들러 연결

## Stub/Mock
- 실제 PG 연동 (가짜 어댑터 사용)
- 정산 (Phase 5에서 구현)

---

## 검증 항목

- [ ] 결제 상태 전이 테스트
- [ ] 부분 환불 금액 검증 (환불 총액 ≤ 결제 금액)
- [ ] Saga 성공: 주문 → 결제 성공 → PAID, 재고 확정
- [ ] Saga 실패: 주문 → 결제 실패 → CANCELLED, 재고 해제
- [ ] 멱등성: 같은 키로 두 번 결제 → 중복 없음
- [ ] 타임아웃: X분 내 미완료 → 자동 취소
