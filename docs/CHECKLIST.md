# 이커머스 도메인 스터디 - 학습 체크리스트

> 각 항목을 완료하면 `[ ]`를 `[x]`로 변경하세요.

---

## Phase 0: DDD 기반 설정 (1주차)

### 도메인 개념 이해
- [ ] Entity vs Value Object 차이를 설명할 수 있다
- [ ] Aggregate와 Aggregate Root의 역할을 이해한다
- [ ] Bounded Context가 왜 필요한지 설명할 수 있다
- [ ] 유비쿼터스 언어의 개념을 이해한다

### 구현
- [ ] Gradle 멀티모듈 프로젝트 스켈레톤 생성
- [ ] `Money` Value Object 구현 (amount + currency, 불변)
- [ ] `Address` Value Object 구현
- [ ] `Quantity` Value Object 구현 (음수 불가)
- [ ] `DomainEvent` 베이스 클래스 구현
- [ ] `AggregateRoot` 베이스 클래스 구현 (이벤트 관리)

### 검증
- [ ] Value Object 동등성 테스트 통과
- [ ] Value Object 불변성 테스트 통과
- [ ] AggregateRoot 이벤트 등록/수집 테스트 통과
- [ ] 전체 Gradle 멀티모듈 빌드 성공

---

## Phase 1: 카탈로그 Context (2-3주차)

### 도메인 개념 이해
- [ ] 상품 생명주기 (Draft → Active → Discontinued)를 이해한다
- [ ] 상품 옵션과 SKU의 관계를 설명할 수 있다
- [ ] 카테고리 계층 구조를 이해한다
- [ ] 가격이 단순 숫자가 아닌 복합 개념임을 이해한다

### 구현
- [ ] `Product` Aggregate Root 구현 (생명주기 포함)
- [ ] `ProductOption`, `OptionValue`, `SKU` 구현
- [ ] `Category` Aggregate Root 구현 (계층 구조)
- [ ] `ProductRepository` 인터페이스 정의
- [ ] 인메모리 Repository 구현
- [ ] `RegisterProductUseCase` 애플리케이션 서비스
- [ ] `ActivateProductUseCase` 애플리케이션 서비스
- [ ] REST API: `POST /products`, `GET /products/{id}`, `PATCH /products/{id}/activate`

### 검증
- [ ] SKU 0개 상품 활성화 불가 테스트
- [ ] 중복 옵션 조합 방지 테스트
- [ ] 가격 양수 검증 테스트
- [ ] 상태 전이 규칙 테스트 (Draft→Discontinued 직접 전이 불가)
- [ ] 도메인 이벤트 발행 테스트
- [ ] 상품 등록→활성화→조회 통합 테스트

---

## Phase 2: 재고 Context (3-4주차)

### 도메인 개념 이해
- [ ] 재고 vs 가용수량 차이를 설명할 수 있다
- [ ] 재고 예약이 왜 필요한지 이해한다
- [ ] 예약 만료 메커니즘을 이해한다
- [ ] 카탈로그와 재고를 분리하는 이유를 설명할 수 있다

### 구현
- [ ] `InventoryItem` Aggregate Root 구현
- [ ] `Reservation` Entity 구현 (예약/확정/취소/만료)
- [ ] 재고 조정 기능 (입고, 차감)
- [ ] 낙관적 잠금 (version 필드)
- [ ] `ProductRegistered` 이벤트 리스너 → InventoryItem 자동 생성
- [ ] 만료 예약 정리 스케줄러

### 검증
- [ ] 가용수량 초과 예약 불가 테스트
- [ ] 예약 확정 시 재고 영구 차감 테스트
- [ ] 취소 시 가용수량 복귀 테스트
- [ ] 만료 예약 자동 해제 테스트
- [ ] 동시성 테스트 (마지막 1개에 2건 동시 예약 → 1건 실패)
- [ ] 카탈로그→재고 이벤트 연동 통합 테스트

---

## Phase 3: 주문 Context (5-7주차)

### 도메인 개념 이해
- [ ] 주문이 CRUD가 아닌 상태 머신임을 이해한다
- [ ] 주문 생명주기 전체를 설명할 수 있다
- [ ] 가격 스냅샷이 왜 필요한지 설명할 수 있다
- [ ] Saga/Process Manager 패턴을 개념적으로 이해한다

### 구현
- [ ] `Order` Aggregate Root 구현 (상태 머신)
- [ ] `OrderLine` Entity 구현 (가격 스냅샷 포함)
- [ ] 상태 전이 검증 로직
- [ ] `OrderService` 도메인 서비스 (주문 생성 오케스트레이션)
- [ ] `PlaceOrderUseCase` 애플리케이션 서비스
- [ ] `CancelOrderUseCase` 애플리케이션 서비스
- [ ] 결제 완료/실패 이벤트 핸들러
- [ ] REST API: 주문 CRUD 및 상태 전이

### 검증
- [ ] 모든 유효 상태 전이 테스트
- [ ] 모든 무효 상태 전이 예외 테스트
- [ ] 배송 후 취소 불가 테스트
- [ ] 가격 스냅샷 불변성 테스트
- [ ] 주문 생성 → 재고 예약 → 취소 → 재고 해제 통합 테스트
- [ ] 멱등성 테스트

---

## Phase 4: 결제 Context (7-9주차)

### 도메인 개념 이해
- [ ] 결제의 복잡한 생명주기를 이해한다
- [ ] 인증(Authorization) vs 매입(Capture) 차이를 설명할 수 있다
- [ ] 환불이 "결제 취소"와 다름을 이해한다
- [ ] 멱등성 키의 필요성을 설명할 수 있다

### 구현
- [ ] `Payment` Aggregate Root 구현 (상태 머신)
- [ ] `Refund` Entity 구현 (부분/전체 환불)
- [ ] `PaymentGateway` 포트 (인터페이스)
- [ ] 가짜 PG 어댑터 구현
- [ ] `InitiatePaymentUseCase` 애플리케이션 서비스
- [ ] `ConfirmPaymentUseCase` 애플리케이션 서비스
- [ ] `RequestRefundUseCase` 애플리케이션 서비스
- [ ] 주문-결제 Saga (Process Manager) 구현
- [ ] 결제 실패 시 보상 트랜잭션 (재고 예약 취소, 주문 취소)

### 검증
- [ ] 결제 상태 전이 테스트
- [ ] 부분 환불 금액 검증 테스트
- [ ] Saga 성공 경로: 주문 → 결제 성공 → PAID, 재고 확정
- [ ] Saga 실패 경로: 주문 → 결제 실패 → CANCELLED, 재고 해제
- [ ] 멱등성 테스트 (같은 키 → 중복 결제 방지)
- [ ] 타임아웃 자동 취소 테스트

---

## Phase 5: 정산 Context (9-10주차)

### 도메인 개념 이해
- [ ] 결제(고객→플랫폼)와 정산(플랫폼→셀러)의 차이를 설명할 수 있다
- [ ] 정산 주기와 배치 처리를 이해한다
- [ ] 수수료 차감 구조를 이해한다
- [ ] 대사(Reconciliation) 개념을 이해한다

### 구현
- [ ] `Settlement` Aggregate Root 구현
- [ ] `SettlementEntry` Entity 구현
- [ ] `CommissionPolicy` 도메인 정책
- [ ] 정산 배치 잡 구현
- [ ] 환불 차감 로직
- [ ] `CalculateSettlementUseCase` 애플리케이션 서비스
- [ ] `ApproveSettlementUseCase` 애플리케이션 서비스

### 검증
- [ ] 여러 결제+환불 포함 정산 계산 테스트
- [ ] 수수료 계산 정확성 테스트
- [ ] 결제 완료 → 다음 정산 배치 포함 통합 테스트
- [ ] 정산 계산 후/지급 전 환불 발생 엣지 케이스 테스트

---

## Phase 6: 배송 Context (10-12주차)

### 도메인 개념 이해
- [ ] 배송이 단순 발송이 아닌 전체 물리적 프로세스임을 이해한다
- [ ] 분할 배송의 필요성을 이해한다
- [ ] 반품/교환의 역물류 플로우를 이해한다

### 구현
- [ ] `Fulfillment` Aggregate Root 구현
- [ ] `Shipment` Entity 구현 (배송 추적)
- [ ] 분할 배송 지원
- [ ] `OrderPaid` → Fulfillment 생성 이벤트 리스너
- [ ] 배송 상태 머신
- [ ] 반품 플로우 (ReturnRequested → ReturnReceived → 환불 트리거)
- [ ] 배송→주문 상태 연동 (ShipmentDispatched → Order SHIPPED)

### 검증
- [ ] 배송 상태 전이 테스트
- [ ] 분할 배송: 모든 Shipment 완료 시에만 DELIVERED 테스트
- [ ] 주문 결제 → 배송 생성 → 출고 → 완료 통합 테스트
- [ ] 반품 E2E (반품 → 환불 → 정산 차감) 테스트

---

## Phase 7: 회원 & 마케팅 Context (12-14주차)

### 도메인 개념 이해
- [ ] 회원(Member)과 유저(User) 분리 이유를 설명할 수 있다
- [ ] 회원 등급 체계를 이해한다
- [ ] 쿠폰 vs 프로모션 차이를 설명할 수 있다
- [ ] 할인 적용 순서와 중복 규칙을 이해한다

### 구현
- [ ] `Member` Aggregate Root 구현 (등급, 포인트)
- [ ] `Coupon` Aggregate Root 구현 (발급/사용/만료)
- [ ] `Promotion` Aggregate Root 구현 (기간, 대상)
- [ ] 등급 자동 계산 (`OrderCompleted` 이벤트 기반)
- [ ] 포인트 적립/사용 로직
- [ ] 프로모션 엔진 (할인 계산)
- [ ] 마케팅→주문 ACL (할인 적용)

### 검증
- [ ] 등급 계산 규칙 테스트
- [ ] 쿠폰 유효성 테스트 (만료, 사용 완료, 최소 주문금액)
- [ ] 할인 중복 적용 규칙 테스트
- [ ] 주문 → 포인트 적립 → 등급 업그레이드 통합 테스트

---

## Phase 8: 전체 통합 (14-16주차)

### 구현
- [ ] 장바구니(Cart) 서브도메인 추가
- [ ] 인프로세스 이벤트 → 비동기 메시징 전환
- [ ] 전역 예외 처리 패턴 적용
- [ ] API 문서화 (OpenAPI/Swagger)

### E2E 플로우 검증
- [ ] 셀러 상품 등록 → 재고 자동 생성 → 입고
- [ ] 고객 회원가입 → 쿠폰 발급
- [ ] 주문 생성 (쿠폰 적용) → 재고 예약
- [ ] 결제 완료 → 주문 PAID
- [ ] 배송 (피킹→포장→출고→배송완료)
- [ ] 주문 DELIVERED → COMPLETED
- [ ] 포인트 적립 → 등급 재평가
- [ ] 결제가 정산 배치에 포함
- [ ] 반품 → 환불 → 정산 차감 → 재고 복귀

### 아키텍처 검증
- [ ] 모든 컨텍스트 경계가 깨끗한가 (크로스 모듈 도메인 모델 import 없음)
- [ ] 컨텍스트 간 통신이 이벤트 또는 ACL로만 이루어지는가
- [ ] 각 Bounded Context가 독립적으로 테스트 가능한가

---

## 전체 완료 조건

- [ ] 8개 Bounded Context 모두 구현 완료
- [ ] 모든 단위 테스트 통과
- [ ] 모든 통합 테스트 통과
- [ ] E2E 전체 플로우 테스트 통과
- [ ] API 문서화 완료
- [ ] 각 Phase별 도메인 개념을 자신의 말로 설명할 수 있다
