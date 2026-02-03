package com.shopfast.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class CategoryClient {

    private final WebClient webClient;
    private final String categoryServiceUrl;

    public CategoryClient(WebClient.Builder builder,
                          @Value("${category.service.url}") String categoryServiceUrl) {
        this.webClient = builder.build();
        this.categoryServiceUrl = categoryServiceUrl;
    }

    public boolean validateCategoryExists(String categoryId) {
        try {
            webClient.get()
                    .uri(categoryServiceUrl + "/" + categoryId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("✅ Category {} validated successfully", categoryId);
            return true;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("❌ Category {} not found in Category Service", categoryId);
            return false;
        } catch (Exception e) {
            log.error("⚠️ Category Service not reachable: {}", e.getMessage());
            return false; // optional: fail open or fail safe
        }
    }
}