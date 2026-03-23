package com.ecommerce.catalog.domain.model;

import com.ecommerce.catalog.domain.event.PriceChanged;
import com.ecommerce.catalog.domain.event.ProductActivated;
import com.ecommerce.catalog.domain.event.ProductDiscontinued;
import com.ecommerce.catalog.domain.event.ProductRegistered;
import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    // ── 테스트 헬퍼 ──

    private Product createDraftProduct() {
        return Product.register("나이키 에어맥스 90", "편안한 운동화", new SellerId("seller-1"));
    }

    /**
     * 옵션이 정의된 DRAFT 상품 생성.
     * SKU 검증이 옵션을 참조하므로 옵션을 먼저 추가해야 한다.
     */
    private Product createDraftProductWithOptions() {
        Product product = createDraftProduct();
        product.addOption(new ProductOption("사이즈", List.of(new OptionValue("270"), new OptionValue("280"))));
        product.addOption(new ProductOption("컬러", List.of(new OptionValue("블랙"), new OptionValue("화이트"))));
        return product;
    }

    private SKU createSku(Map<String, String> optionCombination, long price) {
        Map<String, OptionValue> combination = new java.util.HashMap<>();
        optionCombination.forEach((k, v) -> combination.put(k, new OptionValue(v)));
        return new SKU(SkuId.generate(), combination, Money.krw(price));
    }

    private Product createActiveProduct() {
        Product product = createDraftProductWithOptions();
        product.addSku(createSku(Map.of("사이즈", "270", "컬러", "블랙"), 139000));
        product.activate();
        product.clearEvents();
        return product;
    }

    // ── 상품 등록 ──

    @Nested
    @DisplayName("상품 등록")
    class Registration {

        @Test
        @DisplayName("상품을 등록하면 DRAFT 상태로 시작한다")
        void register_createsWithDraftStatus() {
            Product product = createDraftProduct();

            assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
            assertThat(product.getName()).isEqualTo("나이키 에어맥스 90");
            assertThat(product.getProductId()).isNotNull();
        }

        @Test
        @DisplayName("상품 등록 시 ProductRegistered 이벤트가 발행된다")
        void register_publishesEvent() {
            Product product = createDraftProduct();

            assertThat(product.getEvents()).hasSize(1);
            assertThat(product.getEvents().get(0)).isInstanceOf(ProductRegistered.class);

            ProductRegistered event = (ProductRegistered) product.getEvents().get(0);
            assertThat(event.getName()).isEqualTo("나이키 에어맥스 90");
            assertThat(event.getSellerId()).isEqualTo("seller-1");
        }

        @Test
        @DisplayName("상품명이 없으면 등록할 수 없다")
        void register_withoutName_throwsException() {
            assertThatThrownBy(() -> Product.register("", "설명", new SellerId("seller-1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── 상품 활성화 ──

    @Nested
    @DisplayName("상품 활성화")
    class Activation {

        @Test
        @DisplayName("SKU가 있는 DRAFT 상품을 활성화할 수 있다")
        void activate_withSkus_succeeds() {
            Product product = createDraftProductWithOptions();
            product.addSku(createSku(Map.of("사이즈", "270", "컬러", "블랙"), 139000));

            product.activate();

            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("활성화 시 ProductActivated 이벤트가 발행된다")
        void activate_publishesEvent() {
            Product product = createDraftProductWithOptions();
            product.addSku(createSku(Map.of("사이즈", "270", "컬러", "블랙"), 139000));
            product.clearEvents();

            product.activate();

            List<DomainEvent> events = product.getEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(ProductActivated.class);

            ProductActivated event = (ProductActivated) events.get(0);
            assertThat(event.getSkuIds()).hasSize(1);
        }

        @Test
        @DisplayName("SKU가 없으면 활성화할 수 없다 (핵심 불변식)")
        void activate_withoutSkus_throwsException() {
            Product product = createDraftProduct();

            assertThatThrownBy(product::activate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SKU가 최소 1개");
        }

        @Test
        @DisplayName("이미 ACTIVE인 상품은 다시 활성화할 수 없다")
        void activate_alreadyActive_throwsException() {
            Product product = createActiveProduct();

            assertThatThrownBy(product::activate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT 상태에서만");
        }
    }

    // ── 상품 단종 ──

    @Nested
    @DisplayName("상품 단종")
    class Discontinuation {

        @Test
        @DisplayName("ACTIVE 상품을 단종할 수 있다")
        void discontinue_activeProduct_succeeds() {
            Product product = createActiveProduct();

            product.discontinue();

            assertThat(product.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        }

        @Test
        @DisplayName("단종 시 ProductDiscontinued 이벤트가 발행된다")
        void discontinue_publishesEvent() {
            Product product = createActiveProduct();

            product.discontinue();

            assertThat(product.getEvents()).hasSize(1);
            assertThat(product.getEvents().get(0)).isInstanceOf(ProductDiscontinued.class);
        }

        @Test
        @DisplayName("DRAFT 상품은 단종할 수 없다 (DRAFT→DISCONTINUED 직접 전이 불가)")
        void discontinue_draftProduct_throwsException() {
            Product product = createDraftProduct();

            assertThatThrownBy(product::discontinue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTIVE 상태에서만");
        }
    }

    // ── SKU 관리 ──

    @Nested
    @DisplayName("SKU 관리")
    class SkuManagement {

        @Test
        @DisplayName("같은 옵션 조합의 SKU를 중복 추가할 수 없다")
        void addSku_duplicateCombination_throwsException() {
            Product product = createDraftProductWithOptions();
            product.addSku(createSku(Map.of("사이즈", "270", "컬러", "블랙"), 139000));

            assertThatThrownBy(() -> product.addSku(createSku(Map.of("사이즈", "270", "컬러", "블랙"), 149000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("동일한 옵션 조합");
        }

        @Test
        @DisplayName("다른 옵션 조합의 SKU는 추가할 수 있다")
        void addSku_differentCombination_succeeds() {
            Product product = createDraftProductWithOptions();
            product.addSku(createSku(Map.of("사이즈", "270", "컬러", "블랙"), 139000));
            product.addSku(createSku(Map.of("사이즈", "280", "컬러", "블랙"), 139000));

            assertThat(product.getSkus()).hasSize(2);
        }

        @Test
        @DisplayName("단종된 상품에는 SKU를 추가할 수 없다")
        void addSku_discontinuedProduct_throwsException() {
            Product product = createActiveProduct();
            product.discontinue();

            assertThatThrownBy(() -> product.addSku(createSku(Map.of("사이즈", "280", "컬러", "화이트"), 139000)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Product에 정의되지 않은 옵션으로 SKU를 추가하면 예외 발생")
        void addSku_undefinedOption_throwsException() {
            Product product = createDraftProductWithOptions(); // 사이즈, 컬러 정의됨

            // "무게"는 정의되지 않은 옵션
            assertThatThrownBy(() -> product.addSku(createSku(Map.of("사이즈", "270", "무게", "500g"), 139000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("일치하지 않습니다");
        }

        @Test
        @DisplayName("옵션 개수가 맞지 않으면 예외 발생 (컬러 누락)")
        void addSku_missingOption_throwsException() {
            Product product = createDraftProductWithOptions(); // 사이즈, 컬러 정의됨

            // 사이즈만 있고 컬러가 없음
            assertThatThrownBy(() -> product.addSku(createSku(Map.of("사이즈", "270"), 139000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("일치하지 않습니다");
        }

        @Test
        @DisplayName("옵션에 정의되지 않은 값으로 SKU를 추가하면 예외 발생")
        void addSku_invalidOptionValue_throwsException() {
            Product product = createDraftProductWithOptions(); // 사이즈: 270, 280

            // "999"는 사이즈 옵션값에 없음
            assertThatThrownBy(() -> product.addSku(createSku(Map.of("사이즈", "999", "컬러", "블랙"), 139000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 값");
        }
    }

    // ── 가격 변경 ──

    @Nested
    @DisplayName("가격 변경")
    class PriceChange {

        @Test
        @DisplayName("SKU 가격을 변경하면 PriceChanged 이벤트가 발행된다")
        void changePrice_publishesEvent() {
            Product product = createDraftProductWithOptions();
            SKU sku = createSku(Map.of("사이즈", "270", "컬러", "블랙"), 139000);
            product.addSku(sku);
            product.clearEvents();

            product.changeSkuPrice(sku.getSkuId(), Money.krw(129000));

            assertThat(product.getEvents()).hasSize(1);
            PriceChanged event = (PriceChanged) product.getEvents().get(0);
            assertThat(event.getOldPrice()).isEqualTo(Money.krw(139000));
            assertThat(event.getNewPrice()).isEqualTo(Money.krw(129000));
        }

        @Test
        @DisplayName("존재하지 않는 SKU의 가격을 변경하면 예외 발생")
        void changePrice_unknownSku_throwsException() {
            Product product = createDraftProductWithOptions();

            assertThatThrownBy(() -> product.changeSkuPrice(SkuId.generate(), Money.krw(100000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SKU를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("가격은 양수여야 한다")
        void changePrice_nonPositive_throwsException() {
            Product product = createDraftProductWithOptions();
            SKU sku = createSku(Map.of("사이즈", "270", "컬러", "블랙"), 139000);
            product.addSku(sku);

            assertThatThrownBy(() -> product.changeSkuPrice(sku.getSkuId(), Money.krw(0)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
