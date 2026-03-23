package com.ecommerce.catalog.interfaces.rest;

import com.ecommerce.catalog.application.ActivateProductUseCase;
import com.ecommerce.catalog.application.RegisterProductUseCase;
import com.ecommerce.catalog.application.RegisterProductUseCase.*;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 상품 REST API (인바운드 어댑터).
 *
 * Controller는 HTTP 요청을 Application Service의 Command로 변환하는 역할만 한다.
 * 비즈니스 로직은 여기에 없다.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final RegisterProductUseCase registerProductUseCase;
    private final ActivateProductUseCase activateProductUseCase;
    private final ProductRepository productRepository;

    public ProductController(
            RegisterProductUseCase registerProductUseCase,
            ActivateProductUseCase activateProductUseCase,
            ProductRepository productRepository
    ) {
        this.registerProductUseCase = registerProductUseCase;
        this.activateProductUseCase = activateProductUseCase;
        this.productRepository = productRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterProductRequest request) {
        RegisterProductCommand command = new RegisterProductCommand(
                request.name(),
                request.description(),
                request.sellerId(),
                request.options().stream()
                        .map(o -> new OptionCommand(o.name(), o.values()))
                        .toList(),
                request.skus().stream()
                        .map(s -> new SkuCommand(s.optionCombination(), s.price()))
                        .toList()
        );

        ProductId productId = registerProductUseCase.execute(command);

        return ResponseEntity
                .created(URI.create("/products/" + productId.getId()))
                .body(Map.of("productId", productId.getId()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String productId) {
        Product product = productRepository.findById(new ProductId(productId))
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PatchMapping("/{productId}/activate")
    public ResponseEntity<Void> activate(@PathVariable String productId) {
        activateProductUseCase.execute(productId);
        return ResponseEntity.ok().build();
    }

    // ── Request/Response DTO ──

    public record RegisterProductRequest(
            String name,
            String description,
            String sellerId,
            List<OptionRequest> options,
            List<SkuRequest> skus
    ) {}

    public record OptionRequest(String name, List<String> values) {}

    public record SkuRequest(Map<String, String> optionCombination, long price) {}

    public record ProductResponse(
            String productId,
            String name,
            String description,
            String status,
            String sellerId,
            List<SkuResponse> skus
    ) {
        static ProductResponse from(Product product) {
            List<SkuResponse> skuResponses = product.getSkus().stream()
                    .map(sku -> new SkuResponse(
                            sku.getSkuId().getId(),
                            sku.getOptionCombination().entrySet().stream()
                                    .collect(java.util.stream.Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> e.getValue().getValue()
                                    )),
                            sku.getPrice().getAmount().longValue()
                    ))
                    .toList();

            return new ProductResponse(
                    product.getProductId().getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getStatus().name(),
                    product.getSellerId().getId(),
                    skuResponses
            );
        }
    }

    public record SkuResponse(String skuId, Map<String, String> optionCombination, long price) {}
}
