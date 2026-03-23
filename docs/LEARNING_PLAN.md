# 이커머스 도메인 스터디 - 학습 플랜

## 개요

이커머스의 전체 흐름(상품→주문→결제→배송→정산)을 **도메인 주도 설계(DDD)** 방식으로 학습하는 프로젝트.
각 단계마다 도메인 개념을 먼저 이해한 후, Bounded Context를 정의하고, Aggregate/Entity/Value Object를 설계한 뒤 구현한다.

- **기술 스택:** Java 17+, Spring Boot 3.x, Gradle (멀티모듈)
- **예상 기간:** 12-16주 (파트타임 학습 기준)
- **학습 방식:** 도메인 개념 설명 → DDD 설계 → 코드 구현

---

## Context Map

```
┌─────────────────────────────────────────────────────────────────────┐
│                     이커머스 Context Map                             │
│                                                                     │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────────┐  │
│  │  카탈로그  │───▶│   주문   │───▶│   결제   │───▶│    정산      │  │
│  │ Context  │    │ Context  │    │ Context  │    │   Context    │  │
│  └────┬─────┘    └────┬─────┘    └──────────┘    └──────────────┘  │
│       │               │                                             │
│       ▼               ▼                                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────────┐  │
│  │   재고   │    │   배송    │    │   회원   │    │   마케팅      │  │
│  │ Context  │    │ Context  │    │ Context  │    │   Context    │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### Context 간 관계

| Upstream | Downstream | 패턴 | 핵심 이벤트 |
|----------|-----------|------|------------|
| 카탈로그 | 주문 | Conformist | `ProductRegistered`, `PriceChanged` |
| 카탈로그 | 재고 | Partnership | `ProductRegistered` |
| 주문 | 결제 | Customer-Supplier | `OrderPlaced`, `OrderCancelled` |
| 주문 | 배송 | Customer-Supplier | `OrderPaid`, `OrderCancelled` |
| 결제 | 주문 | 도메인 이벤트 (비동기) | `PaymentCompleted`, `PaymentFailed` |
| 결제 | 정산 | Published Language | `PaymentCompleted`, `RefundCompleted` |
| 배송 | 주문 | 도메인 이벤트 (비동기) | `ShipmentDispatched`, `DeliveryCompleted` |
| 회원 | 주문, 마케팅 | Open Host Service | `MemberRegistered`, `MemberGradeChanged` |
| 마케팅 | 주문 | Anti-Corruption Layer | `CouponIssued`, `PromotionActivated` |

---

## 페이즈 의존관계

```
Phase 0 (기반) ──▶ Phase 1 (카탈로그) ──▶ Phase 2 (재고)
                                                  │
                                                  ▼
                                          Phase 3 (주문) ──┬──▶ Phase 4 (결제)
                                                          │         │
                                                          │         ▼
                                                          │   Phase 5 (정산)
                                                          │
                                                          ├──▶ Phase 6 (배송)
                                                          │
                                                          └──▶ Phase 7 (회원/마케팅)
                                                                      │
                                                     All ──────▶ Phase 8 (통합)
```

**핵심 경로**: Phase 0 → 1 → 2 → 3 → 4
**Phase 3 이후 병렬 가능**: Phase 5, 6, 7은 순서 무관
**Phase 8**: 모든 이전 Phase 완료 필요

---

## 페이즈별 상세 내용

각 Phase의 상세 내용은 `docs/phases/` 디렉토리에서 확인할 수 있습니다.

| Phase | 파일 | 주제 | 기간 |
|-------|------|------|------|
| 0 | [phase-0-foundation.md](phases/phase-0-foundation.md) | DDD 기반 설정 | 1주차 |
| 1 | [phase-1-catalog.md](phases/phase-1-catalog.md) | 카탈로그 (상품, 카테고리) | 2-3주차 |
| 2 | [phase-2-inventory.md](phases/phase-2-inventory.md) | 재고 (예약, 입출고) | 3-4주차 |
| 3 | [phase-3-order.md](phases/phase-3-order.md) | 주문 (상태 머신) | 5-7주차 |
| 4 | [phase-4-payment.md](phases/phase-4-payment.md) | 결제 (PG, 환불, Saga) | 7-9주차 |
| 5 | [phase-5-settlement.md](phases/phase-5-settlement.md) | 정산 (수수료, 배치) | 9-10주차 |
| 6 | [phase-6-fulfillment.md](phases/phase-6-fulfillment.md) | 배송 (출고, 반품) | 10-12주차 |
| 7 | [phase-7-member-marketing.md](phases/phase-7-member-marketing.md) | 회원/마케팅 | 12-14주차 |
| 8 | [phase-8-integration.md](phases/phase-8-integration.md) | 전체 통합 | 14-16주차 |

---

## 참고 자료

| Phase | 개념 | 참고 |
|-------|------|------|
| 0 | DDD 빌딩 블록 | "Domain-Driven Design" (Evans), 5-6장 |
| 0 | Aggregate 설계 | Vaughn Vernon "Effective Aggregate Design" (3부작) |
| 1-2 | Bounded Context | "Domain-Driven Design" (Evans), 14장 |
| 3 | 상태 머신 | "Domain Modeling Made Functional" (Wlaschin) |
| 4 | Saga | "Microservices Patterns" (Richardson), 4장 |
| 5 | 이벤트 주도 | "Implementing Domain-Driven Design" (Vernon), 8장 |
