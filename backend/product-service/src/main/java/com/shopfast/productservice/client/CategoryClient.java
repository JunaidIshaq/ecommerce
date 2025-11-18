package com.shopfast.productservice.client;

import com.shopfast.common.dto.CategoryDto;
import com.shopfast.common.dto.PagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
public class CategoryClient {

    private final WebClient webClient;
    private final String categoryServiceUrl;

    public CategoryClient(WebClient.Builder builder,
                          @Value("${category.service.url}") String categoryServiceUrl) {

        this.webClient = builder
                .baseUrl(categoryServiceUrl)
                .build();

        this.categoryServiceUrl = categoryServiceUrl;
    }

    /**
     * Fetch all categories with safe fallback and retry.
     */
    public PagedResponse<CategoryDto> getAllCategories() {

        int maxRetries = 5;

        for (int i = 1; i <= maxRetries; i++) {
            try {
                log.info("🔄 Fetching categories (attempt {}/{}) from {}", i, maxRetries, categoryServiceUrl);

                return webClient.get()
                        .uri("?pageNumber=1&pageSize=30")
                        .retrieve()
                        .bodyToMono(PagedResponse.class)
                        .block(Duration.ofSeconds(5));

            } catch (WebClientResponseException.NotFound e) {
                log.warn("❌ Category endpoint returned 404: {}", e.getMessage());
                break; // no retry on 404
            } catch (Exception e) {
                log.warn("⚠️ Attempt {} failed: {}", i, e.getMessage());
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {}
            }
        }

        // Final fallback - NEVER return null
        log.error("❌ Category service unreachable. Using fallback empty response.");

        return new PagedResponse<>(
                Collections.emptyList(),
                0,
                0,
                1,
                30
        );
    }

    /**
     * Validate specific category exists.
     */
    public boolean validateCategoryExists(String categoryId) {
        try {
            webClient.get()
                    .uri("/{id}", categoryId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(3));
            log.info("✅ Category {} validated successfully", categoryId);
            return true;

        } catch (WebClientResponseException.NotFound e) {
            log.warn("❌ Category {} not found", categoryId);
            return false;

        } catch (Exception e) {
            log.error("⚠️ Category Service not reachable: {}", e.getMessage());
            return false;
        }
    }
}
