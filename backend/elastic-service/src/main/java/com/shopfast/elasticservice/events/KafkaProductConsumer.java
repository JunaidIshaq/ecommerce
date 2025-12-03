package com.shopfast.elasticservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.common.events.ProductEvent;
import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.repository.ProductSearchRepository;
import com.shopfast.elasticservice.service.EmbeddingService;
import com.shopfast.elasticservice.service.ProductIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KafkaProductConsumer {
    
    private final ObjectMapper objectMapper;
    
    private final EmbeddingService embeddingService;

    private final ProductIndexService productIndexService;

    private final ProductSearchRepository productSearchRepository;

    public KafkaProductConsumer(ObjectMapper objectMapper, EmbeddingService embeddingService, ProductIndexService productIndexService, ProductSearchRepository productSearchRepository) {
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
        this.productIndexService = productIndexService;
        this.productSearchRepository = productSearchRepository;
    }

    @KafkaListener(topics = "product.events", groupId = "elastic-service-group")
    public void consume(ProductEvent event) {
        try {
            log.info("Received product event: {}", event);

            switch (event.getEventType()) {
                case "PRODUCT_CREATED":
                case "PRODUCT_UPDATED":
                    indexProduct(event.getPayload());
                    break;

                case "PRODUCT_DELETED":
                    deleteProduct(event.getPayload());
                    break;

                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }

        } catch (Exception ex) {
            log.error("Error processing product event", ex);
        }
    }


    private void indexProduct(Map<String, Object> payload) {
        ProductDocument doc = ProductDocument.builder()
                .id((String) payload.get("id"))
                .name((String) payload.get("name"))
                .description((String) payload.get("description"))
                .category((String) payload.get("category"))
                .brand((String) payload.get("brand"))
                .price(payload.get("price") != null
                        ? Double.valueOf(payload.get("price").toString()) : null)
                .tags((List<String>) payload.get("tags"))
                .build();

        productIndexService.index(doc);
    }


    private void deleteProduct(Map<String, Object> payload) {
        String id = (String) payload.get("id");
        productSearchRepository.deleteById(id);
        log.info("Deleted from index: {}", id);
    }


}
