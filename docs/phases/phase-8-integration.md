# Phase 8: 전체 통합 & 마무리 (14-16주차)

## 목표

- 인프로세스 이벤트를 비동기 메시징으로 전환
- 장바구니(Cart) 서브도메인 추가
- 전체 E2E 플로우 테스트
- API 문서화
- 아키텍처 검증

---

## 장바구니 (Cart)

### 도메인 개념
- 장바구니는 "주문 전 단계"
- 주문 Context 내 서브도메인으로 위치 (별도 Bounded Context까지는 불필요)
- 장바구니 → 주문 전환 시 가격 재검증 필요 (장바구니에 담은 후 가격이 바뀔 수 있음)

### 구현
```
Cart (Aggregate Root 또는 Entity)
├── cartId: CartId
├── memberId: MemberId
├── items: List<CartItem>
│   ├── skuId, quantity
│   └── addedAt
└── 가격은 저장하지 않음 (조회 시 카탈로그에서 실시간 조회)
```

---

## 비동기 이벤트 전환

### 현재 (Phase 1-7)
- Spring `ApplicationEventPublisher` (동기, 같은 트랜잭션)
- 장점: 단순, 디버깅 쉬움
- 단점: 컨텍스트 간 결합도 높음

### 전환 후
- 이벤트 버스 추상화 도입
- `@Async` + `@TransactionalEventListener` 로 비동기 처리
- 또는 Spring Cloud Stream (선택사항)

### 주의: Transactional Outbox 패턴
- 도메인 이벤트를 DB에 먼저 저장 → 별도 프로세스가 발행
- 이벤트 유실 방지
- 이 프로젝트에서는 개념만 이해하고, 단순 `@Async`로 구현

---

## 전역 예외 처리

```
예외 계층:
├── DomainException (도메인 규칙 위반)
│   ├── InvalidStateTransitionException
│   ├── InsufficientStockException
│   ├── InvalidPaymentStateException
│   └── ...
├── ApplicationException (애플리케이션 레벨)
│   ├── OrderNotFoundException
│   ├── PaymentAlreadyExistsException
│   └── ...
└── @RestControllerAdvice → HTTP 상태 코드 매핑
```

---

## 전체 E2E 플로우

```
정상 플로우:
 1. [카탈로그]  셀러가 상품 등록 (SKU 포함)
 2. [재고]     InventoryItem 자동 생성, 입고 처리
 3. [회원]     고객 회원가입
 4. [마케팅]   고객에게 쿠폰 발급
 5. [주문]     고객이 주문 생성 (쿠폰 적용)
    5a. [마케팅] 할인 계산 및 적용
    5b. [재고]  재고 예약
 6. [결제]     결제 시작 및 완료
 7. [주문]     주문 상태 → PAID
 8. [배송]     배송 생성 → 피킹 → 포장 → 출고
 9. [주문]     주문 상태 → SHIPPED
10. [배송]     배송 완료 확인
11. [주문]     DELIVERED → COMPLETED
12. [회원]     포인트 적립, 등급 재평가
13. [정산]     결제가 다음 정산 배치에 포함

반품 플로우:
14. [배송]     반품 요청 및 수령
15. [결제]     환불 처리
16. [정산]     환불 금액 정산에서 차감
17. [재고]     반품 재고 복귀
```

---

## 아키텍처 검증 체크리스트

### 컨텍스트 경계 검증
- [ ] 모든 모듈의 `build.gradle`에서 다른 컨텍스트의 domain 패키지를 import하지 않음
- [ ] 크로스 컨텍스트 참조는 항상 ID(Value Object)로만
- [ ] 컨텍스트 간 통신은 도메인 이벤트 또는 ACL로만

### 독립성 검증
- [ ] 각 Bounded Context를 개별적으로 테스트 가능
- [ ] 하나의 Context를 수정해도 다른 Context의 도메인 모델에 영향 없음

### DDD 패턴 검증
- [ ] 모든 Aggregate가 일관성 경계를 올바르게 정의
- [ ] 모든 상태 변경이 Aggregate Root를 통해서만 이루어짐
- [ ] 도메인 이벤트가 상태 변경의 부수 효과를 올바르게 전달

---

## API 문서화

- OpenAPI/Swagger 설정 (`springdoc-openapi`)
- 각 Context별 API 그룹화
- 요청/응답 예시 포함

---

## 구현할 것

1. 장바구니(Cart) 서브도메인
2. 이벤트 버스 추상화 + 비동기 전환
3. 전역 예외 처리 (`@RestControllerAdvice`)
4. API 문서화 (OpenAPI/Swagger)
5. 전체 E2E 통합 테스트

---

## 검증 항목

- [ ] 장바구니 → 주문 전환 통합 테스트
- [ ] 비동기 이벤트 전달 검증
- [ ] 정상 E2E 전체 플로우 통과
- [ ] 반품 E2E 전체 플로우 통과
- [ ] 아키텍처 검증 체크리스트 전체 통과
- [ ] API 문서 접근 가능 (`/swagger-ui.html`)
