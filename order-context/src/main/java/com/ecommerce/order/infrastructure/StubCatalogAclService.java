package com.ecommerce.order.infrastructure;

import com.ecommerce.common.model.Money;
import com.ecommerce.order.domain.service.CatalogAclService;

/**
 * ★ ACL 구현체 (Stub) ★
 *
 * 실제로는 카탈로그 컨텍스트의 API를 호출하여 상품 정보를 가져온 뒤
 * ProductSnapshot으로 변환해야 한다.
 *
 * 지금은 학습 목적으로 하드코딩된 값을 반환하는 스텁.
 * 나중에 카탈로그 컨텍스트와 실제 연동할 때 이 클래스만 교체하면 된다.
 * → 주문의 도메인 로직은 전혀 바뀌지 않음 (포트/어댑터 패턴의 장점)
 */
public class StubCatalogAclService implements CatalogAclService {

    @Override
    public ProductSnapshot getProductSnapshot(String skuId) {
        // 실제 구현에서는:
        // 1. catalogApi.getSku(skuId) 호출 (카탈로그의 모델)
        // 2. 응답을 ProductSnapshot으로 변환 (ACL의 핵심 역할)
        // 3. 카탈로그의 Product, SKU 클래스를 이 파일 밖으로 노출하지 않음

        return new ProductSnapshot(
                skuId,
                "테스트 상품 (" + skuId + ")",
                Money.krw(10000)
        );
    }
}
