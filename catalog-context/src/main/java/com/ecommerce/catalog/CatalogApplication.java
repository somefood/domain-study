package com.ecommerce.catalog;

import com.ecommerce.catalog.application.ActivateProductUseCase;
import com.ecommerce.catalog.application.RegisterProductUseCase;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import com.ecommerce.catalog.infrastructure.InMemoryProductRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 카탈로그 컨텍스트 애플리케이션.
 *
 * 아직 JPA를 사용하지 않으므로 DataSource 자동설정을 제외.
 * 인메모리 Repository를 사용.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }

    @Bean
    public ProductRepository productRepository() {
        return new InMemoryProductRepository();
    }

    @Bean
    public RegisterProductUseCase registerProductUseCase(ProductRepository productRepository) {
        return new RegisterProductUseCase(productRepository);
    }

    @Bean
    public ActivateProductUseCase activateProductUseCase(ProductRepository productRepository) {
        return new ActivateProductUseCase(productRepository);
    }
}
