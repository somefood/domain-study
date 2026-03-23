# Phase 1: 카탈로그 Context (2-3주차)

## 학습할 도메인 개념

### 상품 생명주기
- **Draft(초안)**: 셀러가 상품 정보를 입력 중. 아직 노출 안 됨
- **Active(활성)**: 판매 중. 고객에게 노출됨
- **Discontinued(단종)**: 더 이상 판매하지 않음

### 상품 옵션과 SKU
- **상품(Product)**: 판매 단위. 예: "나이키 에어맥스"
- **옵션(Option)**: 변형 축. 예: 사이즈, 컬러
- **옵션값(OptionValue)**: 옵션 내 선택지. 예: "270mm", "블랙"
- **SKU(Stock Keeping Unit)**: 옵션 조합의 최소 구매 단위. 예: "나이키 에어맥스 270mm 블랙" → 이것이 실제 재고 추적/가격 단위

### 카테고리 계층
- 트리 구조 (대분류 → 중분류 → 소분류)
- 상품은 하나 이상의 카테고리에 속할 수 있음

### 가격의 복합성
- 기본가(Base Price) vs 판매가(Sale Price)
- 유효기간이 있는 가격 (세일 기간)
- 가격은 SKU 단위로 관리

---

## Bounded Context 정의

**책임**: 상품 정보 관리 — 무엇을 파는가, 어떻게 설명되는가, 가격은 얼마인가

### 유비쿼터스 언어

| 용어 | 정의 |
|------|------|
| Product (상품) | 하나 이상의 옵션을 가진 판매 항목 |
| Option (옵션) | 변형 축 (예: 사이즈, 컬러) |
| OptionValue (옵션값) | 옵션 내 구체적 선택지 (예: "Large", "Red") |
| SKU | 옵션 조합으로 정의된 고유 구매 단위 |
| Category (카테고리) | 상품 탐색을 위한 계층적 분류 |
| Price (가격) | 유효기간이 있는 금액 |
| Product Status (상품 상태) | Draft, Active, Discontinued |

---

## 핵심 Aggregate 설계

### Product (Aggregate Root)
```
Product
├── productId: ProductId (VO)
├── name: String
├── description: String
├── status: ProductStatus (DRAFT, ACTIVE, DISCONTINUED)
├── sellerId: SellerId (VO, ID로만 참조)
├── categoryIds: Set<CategoryId> (ID로만 참조)
├── options: List<ProductOption> (Entity, Product가 소유)
│   ├── optionId, name
│   └── values: List<OptionValue> (VO)
├── skus: List<SKU> (Entity, Product가 소유)
│   ├── skuId, skuCode
│   ├── optionCombination: Map<OptionId, OptionValue>
│   └── price: Money (VO)
└── events: List<DomainEvent>
```

### Category (Aggregate Root)
```
Category
├── categoryId: CategoryId (VO)
├── name: String
├── parentCategoryId: CategoryId? (ID로만 참조)
└── depth: int
```

---

## 도메인 이벤트

| 이벤트 | 발생 시점 | 포함 데이터 |
|--------|----------|------------|
| ProductRegistered | 상품 최초 등록 | productId, name, sellerId |
| ProductActivated | 상품 활성화 | productId, skuIds |
| PriceChanged | 가격 변경 | productId, skuId, oldPrice, newPrice |
| ProductDiscontinued | 상품 단종 | productId |

---

## 구현할 것

1. `Product` Aggregate (생명주기: draft → activate → discontinue)
2. `Category` Aggregate (부모-자식 계층)
3. `ProductService` 도메인 서비스 (가격 검증 규칙)
4. `ProductRepository` 인터페이스
5. 인메모리 Repository 구현 (테스트용)
6. 애플리케이션 서비스: `RegisterProductUseCase`, `ActivateProductUseCase`
7. REST API: `POST /products`, `GET /products/{id}`, `PATCH /products/{id}/activate`

## Stub/Mock
- 셀러 검증 (아무 sellerId나 허용)
- 이미지/미디어 (모델링하지 않음)
- 검색/필터링 (단순 인메모리)

---

## 검증 항목

- [ ] SKU가 0개인 상품은 활성화 불가
- [ ] 중복 옵션 조합 추가 불가
- [ ] 가격은 반드시 양수
- [ ] Draft → Discontinued 직접 전이 불가 (반드시 Active를 거쳐야)
- [ ] 상태 변경 시 도메인 이벤트 발행
- [ ] 상품 등록 → 활성화 → REST API 조회 통합 테스트
