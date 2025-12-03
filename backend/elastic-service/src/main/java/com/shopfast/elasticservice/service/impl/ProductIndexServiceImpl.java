package com.shopfast.elasticservice.service.impl;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.repository.ProductSearchRepository;
import com.shopfast.elasticservice.service.EmbeddingService;
import com.shopfast.elasticservice.service.ProductIndexService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Service
public class ProductIndexServiceImpl implements ProductIndexService {

    private final ProductSearchRepository repository;
    private final EmbeddingService embeddingService;
    private final RestClient restClient; // auto-configured by Spring Data ES

    public ProductIndexServiceImpl(ProductSearchRepository repository, EmbeddingService embeddingService, RestClient restClient) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.restClient = restClient;
    }

    @Override
    @Transactional
    public ProductDocument index(ProductDocument product) {
        // build text for embedding
//        String toEmbed = (product.getName() + " " + product.getDescription()).trim();
//        float[] vector = embeddingService.embed(toEmbed);
//        product.setEmbedding(vector);

        // save other fields via repository
        ProductDocument saved = repository.save(product);
/** TODO Add open embedding
        // update embedding using ES REST API, because dense_vector is not fully supported by repo
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"doc\": {\"embedding\": [");
            for (int i = 0; i < vector.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(vector[i]);
            }
            sb.append("]}}");

            Request request = new Request("POST", "/product/_update/" + saved.getId());
            request.setJsonEntity(sb.toString());
            Response response = restClient.performRequest(request);

            log.info("Updated embedding for product {}. Status: {}", saved.getId(), response.getStatusLine());

        } catch (IOException e) {
            log.error("Failed to update embedding in Elasticsearch", e);
        }
 **/
        return saved;
    }

}
