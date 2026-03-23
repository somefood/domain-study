package com.ecommerce.order.domain.model;

import com.ecommerce.common.model.Money;
import com.ecommerce.common.model.Quantity;

import java.util.Objects;
import java.util.UUID;

/**
 * 주문 항목 Entity.
 *
 * ★ ACL(Anti-Corruption Layer)의 결과물 ★
 *
 * 이 클래스의 필드들(productName, unitPrice)은 카탈로그 컨텍스트에서
 * "복사(스냅샷)"해온 값이다. 카탈로그의 Product나 SKU 클래스를 직접 참조하지 않는다.
 *
 * 왜?
 * - 카탈로그에서 상품명을 바꿔도 이미 주문된 항목의 이름은 바뀌면 안 됨
 * - 카탈로그에서 가격을 올려도 주문 시점의 가격이 유지되어야 함
 * - 주문 컨텍스트가 카탈로그의 내부 모델에 의존하면 결합도가 높아짐
 *
 * 즉, OrderLine은 "주문 당시의 상품 정보 사진"을 찍어서 보관하는 것이다.
 */
public class OrderLine {

    private final String orderLineId;
    private final String skuId;          // 카탈로그의 SKU를 ID로만 참조
    private final String productName;    // 주문 시점의 상품명 (스냅샷)
    private final Money unitPrice;       // 주문 시점의 단가 (스냅샷) ← ACL이 변환해준 값
    private final Quantity quantity;

    public OrderLine(String skuId, String productName, Money unitPrice, Quantity quantity) {
        if (skuId == null || skuId.isBlank()) {
            throw new IllegalArgumentException("SKU ID는 필수입니다");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다");
        }
        if (unitPrice == null || !unitPrice.isPositive()) {
            throw new IllegalArgumentException("단가는 양수여야 합니다");
        }
        if (quantity == null || quantity.isZero()) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
        }
        this.orderLineId = UUID.randomUUID().toString();
        this.skuId = skuId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    /**
     * 이 주문항목의 소계.
     * 단가 × 수량.
     */
    public Money getLineTotal() {
        return unitPrice.multiply(quantity.getValue());
    }

    public String getOrderLineId() { return orderLineId; }
    public String getSkuId() { return skuId; }
    public String getProductName() { return productName; }
    public Money getUnitPrice() { return unitPrice; }
    public Quantity getQuantity() { return quantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLine orderLine = (OrderLine) o;
        return orderLineId.equals(orderLine.orderLineId);
    }

    @Override
    public int hashCode() { return Objects.hash(orderLineId); }
}
