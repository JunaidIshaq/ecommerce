package com.shopfast.elasticservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.service.EmbeddingService;
import com.shopfast.elasticservice.service.ProductSearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    private final EmbeddingService embeddingService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ProductSearchServiceImpl(EmbeddingService embeddingService, RestClient restClient, ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ProductDocument> semanticSearch(String query, int k) {
        float[] vector = embeddingService.embed(query);

        // build KNN query JSON
        StringBuilder vec = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) vec.append(",");
            vec.append(vector[i]);
        }

        String json = """
                {
                  "knn": {
                    "field": "embedding",
                    "query_vector": [%s],
                    "k": %d,
                    "num_candidates": %d
                  }
                }
                """.formatted(vec, k, Math.max(k * 2, 50));

        Request request = new Request("POST", "/product/_knn_search");
        request.setJsonEntity(json);

        try {
            Response response = restClient.performRequest(request);
            InputStream is = response.getEntity().getContent();
            JsonNode root = objectMapper.readTree(is);

            List<ProductDocument> results = new ArrayList<>();
            JsonNode hits = root.path("hits").path("hits");
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                ProductDocument doc = objectMapper.treeToValue(source, ProductDocument.class);
                results.add(doc);
            }
            return results;

        } catch (IOException e) {
            log.error("Error executing KNN search", e);
            return Collections.emptyList();
        }
    }

}
