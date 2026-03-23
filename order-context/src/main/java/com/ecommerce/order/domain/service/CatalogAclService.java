package com.ecommerce.order.domain.service;

import com.ecommerce.common.model.Money;

/**
 * ★ ACL (Anti-Corruption Layer) — 카탈로그 컨텍스트와의 변환 계층 ★
 *
 * ACL이란?
 * 다른 Bounded Context(카탈로그)의 모델이 내 컨텍스트(주문)를 "오염"시키지 않도록
 * 중간에서 변환해주는 계층.
 *
 * 왜 필요한가?
 * - 주문 컨텍스트는 카탈로그의 Product, SKU, ProductOption 같은 클래스를 모른다
 * - 주문에 필요한 건 오직: "이 SKU의 이름이 뭐고 가격이 얼마인가?" 뿐
 * - ACL이 카탈로그의 복잡한 모델을 주문에 필요한 단순한 형태로 번역
 *
 * 동작 방식:
 * 1. 카탈로그 컨텍스트에 "SKU 정보 줘" 요청 (API 호출 또는 인프로세스 호출)
 * 2. 카탈로그가 Product, SKU 등 자기 모델로 응답
 * 3. ACL이 그 응답을 ProductSnapshot(주문의 모델)으로 변환
 * 4. 주문 컨텍스트는 ProductSnapshot만 사용 → 카탈로그 모델에 의존하지 않음
 *
 * 이 인터페이스는 주문의 "도메인 레이어"에 있다.
 * 구현체는 "인프라 레이어"에서 카탈로그를 실제로 호출한다.
 */
public interface CatalogAclService {

    /**
     * SKU의 상품 정보를 주문에 필요한 형태(스냅샷)로 가져온다.
     *
     * 카탈로그의 Product.name, SKU.price 등을 조회하여
     * 주문 컨텍스트에 맞는 ProductSnapshot으로 변환한다.
     *
     * @param skuId 카탈로그의 SKU 식별자
     * @return 주문에 필요한 상품 스냅샷 (이름 + 가격)
     * @throws IllegalArgumentException SKU가 존재하지 않거나 판매 중이 아닌 경우
     */
    ProductSnapshot getProductSnapshot(String skuId);

    /**
     * 주문 컨텍스트가 카탈로그로부터 받아오는 상품 정보.
     *
     * 이것이 ACL의 "출력"이다.
     * 카탈로그의 Product, SKU, ProductOption 등 복잡한 모델을
     * 주문에 필요한 딱 3개 필드로 단순화.
     *
     * 한번 생성되면 변하지 않는다 (스냅샷).
     * 나중에 카탈로그에서 가격을 바꿔도 이 스냅샷은 그대로.
     */
    record ProductSnapshot(
            String skuId,        // 어떤 SKU인지 식별
            String productName,  // 주문 시점의 상품명
            Money unitPrice      // 주문 시점의 가격 ← 이게 가격 스냅샷의 핵심
    ) {}
}
