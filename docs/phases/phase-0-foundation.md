# Phase 0: DDD 기반 설정 (1주차)

## 학습할 도메인 개념

### Entity vs Value Object
- **Entity**: 고유한 식별자(ID)로 구분. 속성이 바뀌어도 같은 객체. 예: 주문(Order)은 상태가 바뀌어도 같은 주문
- **Value Object**: 값 자체로 비교. 불변(immutable). 예: `Money(1000, KRW)` 두 개는 동일한 객체

### Aggregate와 Aggregate Root
- **Aggregate**: 하나의 단위로 취급되는 도메인 객체 클러스터. 일관성 경계(consistency boundary)
- **Aggregate Root**: Aggregate 내부 수정의 유일한 진입점. 외부에서는 Root만 참조
- **규칙**: Aggregate 간에는 ID로만 참조 (직접 객체 참조 X)

### Bounded Context
- 같은 단어가 컨텍스트마다 다른 의미를 가짐
- 예: "상품"이 카탈로그에서는 "판매 정보"이고, 재고에서는 "수량 관리 단위"
- 각 컨텍스트는 자기만의 모델과 언어를 가짐

### 유비쿼터스 언어 (Ubiquitous Language)
- 개발자와 도메인 전문가가 같은 용어를 사용
- 코드의 클래스/메서드 이름이 곧 도메인 용어

---

## 구현할 것

### Gradle 멀티모듈 프로젝트 스켈레톤
```
domain-study/
├── build.gradle
├── settings.gradle
├── common/
│   ├── common-types/        # Shared Kernel
│   └── common-infra/        # 공통 인프라
├── catalog-context/
├── inventory-context/
├── order-context/
├── payment-context/
├── settlement-context/
├── fulfillment-context/
├── member-context/
└── marketing-context/
```

### common-types 모듈

**Money (Value Object)**
- amount (BigDecimal) + currency (Currency)
- 불변, 사칙연산 메서드 제공
- 동등성은 amount + currency로 판단

**Address (Value Object)**
- street, city, zipCode
- 불변

**Quantity (Value Object)**
- 음수 불가 정수 래퍼
- 더하기/빼기 연산 시 음수 검증

**DomainEvent (베이스 클래스)**
- eventId (UUID)
- occurredAt (LocalDateTime)
- aggregateId (String)

**AggregateRoot (베이스 클래스)**
- 도메인 이벤트 리스트 관리
- registerEvent(), clearEvents(), getEvents()

---

## 검증 항목

- [ ] `Money(1000, KRW).equals(Money(1000, KRW))` → true
- [ ] `Money`는 생성 후 값 변경 불가
- [ ] `Quantity(-1)` → 예외 발생
- [ ] `AggregateRoot`에 이벤트 등록 후 수집 가능
- [ ] Gradle 멀티모듈 `./gradlew build` 성공

---

## 패키지 구조 컨벤션 (이후 모든 모듈 공통)

```
com.ecommerce.{context}/
├── domain/
│   ├── model/          # Aggregate, Entity, Value Object
│   ├── event/          # 도메인 이벤트
│   ├── service/        # 도메인 서비스
│   ├── repository/     # Repository 인터페이스 (포트)
│   └── policy/         # 도메인 정책
├── application/        # 애플리케이션 서비스 (유스케이스)
├── infrastructure/     # JPA, 외부 API, 메시징
└── interfaces/         # REST 컨트롤러, DTO
```
