# Phase 5: 정산 Context (9-10주차)

## 학습할 도메인 개념

### 결제 vs 정산
- **결제(Payment)**: 고객 → 플랫폼으로 돈이 이동
- **정산(Settlement)**: 플랫폼 → 셀러로 돈이 이동
- 결제는 실시간, 정산은 배치(일/주/월 단위)

### 정산 주기
- 매일/매주/매월 특정 시점에 배치로 처리
- 예: "이번 주 월~일 완료된 결제를 다음 주 수요일에 셀러에게 지급"

### 수수료 (Commission)
- 플랫폼이 셀러 매출에서 일정 비율을 떼감
- 카테고리별, 셀러 등급별로 수수료율이 다를 수 있음
- 정산 금액 = 결제 금액 - 수수료 - 환불 차감

### 정산 보류
- 환불 가능 기간(예: 배송 후 7일) 동안은 정산을 보류
- 사기 의심 거래도 보류 대상

### 대사 (Reconciliation)
- PG사가 보내는 정산 내역과 내부 기록을 대조
- 불일치가 있으면 수동으로 확인/조정

---

## Bounded Context 정의

**책임**: 완료된 결제에서 셀러 지급금을 계산하고 배분

### 유비쿼터스 언어

| 용어 | 정의 |
|------|------|
| Settlement (정산) | 셀러에 대한 지급금 계산 및 이체 |
| Settlement Period (정산 기간) | 정산 대상 기간 (예: 3월 1-7일) |
| Commission (수수료) | 결제 금액에서 플랫폼이 차감하는 비율 |
| Net Amount (순 금액) | 결제 금액 - 수수료 - 환불 = 셀러 수령액 |
| Settlement Status | Pending, Calculated, Approved, Paid |

---

## 핵심 Aggregate 설계

### Settlement (Aggregate Root)
```
Settlement
├── settlementId: SettlementId (VO)
├── sellerId: SellerId (VO)
├── period: SettlementPeriod (VO: startDate, endDate)
├── entries: List<SettlementEntry> (Entity, Settlement이 소유)
│   ├── paymentId: PaymentId
│   ├── orderAmount: Money
│   ├── commissionRate: BigDecimal (예: 0.10 = 10%)
│   ├── commissionAmount: Money
│   ├── refundDeductions: Money
│   └── netAmount: Money
├── totalGrossAmount: Money (파생)
├── totalCommission: Money (파생)
├── totalRefundDeductions: Money (파생)
├── totalNetAmount: Money (파생)
├── status: SettlementStatus
└── events: List<DomainEvent>
```

---

## 도메인 이벤트

| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| SettlementCalculated | 정산 계산 완료 | settlementId, sellerId, period, netAmount |
| SettlementApproved | 정산 승인 | settlementId |
| SettlementPaid | 셀러 지급 완료 | settlementId, paidAt |

---

## 구현할 것

1. `Settlement` Aggregate (배치 계산 로직)
2. `CommissionPolicy` 도메인 정책 (수수료율 결정)
3. 정산 배치 잡: 기간 내 `PaymentCompleted` 수집 → 셀러별 정산 계산
4. 환불 차감: `RefundCompleted` → 해당 정산의 금액 감소
5. `CalculateSettlementUseCase`, `ApproveSettlementUseCase`

## Stub/Mock
- 은행 송금 (실제 이체 X)
- 세금 계산
- 복잡한 수수료 체계 (단일 비율로 단순화)

---

## 검증 항목

- [ ] 여러 결제 + 환불 포함 정산 계산 정확성
- [ ] 수수료 계산 정확성 (금액 × 수수료율)
- [ ] 결제 완료 → 다음 정산 배치에 포함 통합 테스트
- [ ] 엣지 케이스: 정산 계산 후 / 지급 전에 환불 발생
