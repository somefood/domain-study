# Phase 7: 회원 & 마케팅 Context (12-14주차)

## 학습할 도메인 개념

### 회원(Member) ≠ 유저(User)
- **User**: 로그인, 인증, 비밀번호 — 기술적 관심사
- **Member**: 등급, 포인트, 구매 이력 — 비즈니스 관심사
- 이커머스에서 중요한 건 Member. 인증은 별도 시스템(또는 OAuth)에 위임

### 회원 등급 (Grade/Tier)
- 누적 구매 금액 기반으로 자동 계산
  - 브론즈: 0 ~ 100만원
  - 실버: 100만 ~ 500만원
  - 골드: 500만원 이상
- 등급에 따라 혜택이 달라짐 (할인율, 포인트 적립률 등)

### 쿠폰 vs 프로모션
- **쿠폰 (Coupon)**: 특정 회원에게 발급. 1회 사용. 유효기간 있음
  - 예: "신규 가입 10% 할인 쿠폰"
- **프로모션 (Promotion)**: 전체 또는 특정 상품/카테고리 대상. 기간 한정
  - 예: "전자제품 카테고리 20% 할인 (3/1~3/7)"

### 할인 적용 순서
- 모든 할인이 중복 적용 가능한 건 아님
- 일반적 규칙:
  1. 프로모션 할인 먼저 적용
  2. 쿠폰 할인 적용
  3. 포인트 사용
- 정액 할인과 정률 할인의 적용 순서도 중요

---

## Bounded Context 정의

### 회원 Context
**책임**: 고객 신원, 등급, 포인트, 선호도

### 마케팅 Context
**책임**: 프로모션, 쿠폰, 할인 규칙

---

## 핵심 Aggregate 설계

### Member (Aggregate Root) — 회원 Context
```
Member
├── memberId: MemberId (VO)
├── name: String
├── email: Email (VO)
├── phone: PhoneNumber (VO)
├── grade: MemberGrade (VO: BRONZE, SILVER, GOLD)
├── totalPurchaseAmount: Money (누적 구매 금액)
├── point: Point (VO: 현재 보유 포인트)
├── joinedAt: LocalDateTime
└── events: List<DomainEvent>
```

### Coupon (Aggregate Root) — 마케팅 Context
```
Coupon
├── couponId: CouponId (VO)
├── memberId: MemberId (VO)
├── name: String
├── discountPolicy: DiscountPolicy (VO)
│   ├── type: FIXED_AMOUNT | PERCENTAGE
│   ├── value: BigDecimal (정액이면 금액, 정률이면 비율)
│   └── maxDiscountAmount: Money? (정률일 때 최대 할인 한도)
├── minOrderAmount: Money (최소 주문 금액)
├── validFrom: LocalDateTime
├── validUntil: LocalDateTime
├── usedAt: LocalDateTime?
├── status: CouponStatus (ISSUED, USED, EXPIRED)
└── events: List<DomainEvent>
```

### Promotion (Aggregate Root) — 마케팅 Context
```
Promotion
├── promotionId: PromotionId (VO)
├── name: String
├── description: String
├── targetType: PromotionTarget (PRODUCT, CATEGORY, ALL)
├── targetIds: Set<String> (대상 상품/카테고리 ID)
├── discountPolicy: DiscountPolicy (VO)
├── period: DateRange (VO: startDate, endDate)
├── status: PromotionStatus (SCHEDULED, ACTIVE, ENDED)
└── events: List<DomainEvent>
```

---

## 도메인 이벤트

### 회원 이벤트
| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| MemberRegistered | 회원 가입 | memberId, name, email |
| MemberGradeChanged | 등급 변경 | memberId, oldGrade, newGrade |
| PointEarned | 포인트 적립 | memberId, amount, reason |
| PointUsed | 포인트 사용 | memberId, amount, orderId |

### 마케팅 이벤트
| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| CouponIssued | 쿠폰 발급 | couponId, memberId, discountPolicy |
| CouponUsed | 쿠폰 사용 | couponId, memberId, orderId |
| CouponExpired | 쿠폰 만료 | couponId |
| PromotionActivated | 프로모션 시작 | promotionId, period |
| PromotionEnded | 프로모션 종료 | promotionId |

---

## 크로스 컨텍스트 연동

| 연동 | 방향 | 패턴 |
|------|------|------|
| 주문 → 회원 | `OrderCompleted` 구독 | 등급 재계산, 포인트 적립 |
| 마케팅 → 주문 | ACL | 주문이 할인 계산을 요청하되, 마케팅 내부 모델에 의존 X |
| 회원 → 주문 | Open Host Service | 주문이 `MemberId`로 등급 정보 조회 |

---

## 구현할 것

1. `Member` Aggregate (등급, 포인트)
2. 등급 자동 계산 (`OrderCompleted` 이벤트 → 누적 구매금액 갱신 → 등급 재평가)
3. 포인트 적립 (주문 금액의 일정 비율) / 사용
4. `Coupon` Aggregate (발급/검증/사용/만료)
5. `Promotion` Aggregate (기간, 대상)
6. 할인 계산 서비스 (DiscountCalculationService)
7. 마케팅→주문 ACL 연동

## Stub/Mock
- 인증/로그인 (별도 시스템)
- 추천 시스템
- 이메일/알림 발송

---

## 검증 항목

- [ ] 등급 계산: 구매 누적 100만원 → 실버 승급
- [ ] 쿠폰 유효성: 만료된 쿠폰 사용 불가
- [ ] 쿠폰 유효성: 이미 사용한 쿠폰 재사용 불가
- [ ] 쿠폰 유효성: 최소 주문금액 미달 시 사용 불가
- [ ] 할인 중복 적용 규칙 검증
- [ ] 주문 완료 → 포인트 적립 → 등급 업그레이드 통합 테스트
