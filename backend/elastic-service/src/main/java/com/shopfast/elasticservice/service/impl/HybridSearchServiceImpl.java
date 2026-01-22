package com.shopfast.elasticservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.dto.HybridSearchRequestDto;
import com.shopfast.elasticservice.service.EmbeddingService;
import com.shopfast.elasticservice.service.HybridSearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HybridSearchServiceImpl implements HybridSearchService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;

    public HybridSearchServiceImpl(RestClient restClient, ObjectMapper objectMapper, EmbeddingService embeddingService) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
    }

    @Override
    public List<ProductDocument> search(HybridSearchRequestDto requestDto) {
       float[] queryVector = embeddingService.embed(requestDto.getQuery());

        StringBuilder vectorJson = new StringBuilder();
        for (int i = 0; i < queryVector.length; i++) {
            if (i > 0) {
                vectorJson.append(",");
            }
            vectorJson.append(queryVector[i]);
        }

        StringBuilder filters = new StringBuilder();

        if(requestDto.getCategory() != null) {
            filters.append("""
                    { "term" : { "category": "%s"} },
                    """.formatted(requestDto.getCategory()));
        }

        if (requestDto.getBrand() != null) {
            filters.append("""
                    { "term": { "brand": "%s" } },
                    """.formatted(requestDto.getBrand()));
        }

        if(requestDto.getMinPrice() != null || requestDto.getMaxPrice() != null) {
            filters.append("""
                    { "range" : { "price" : {
                       %s
                       %s
                    }}},
                    """.formatted(
                            requestDto.getMinPrice() != null ? "\"gte\": " + requestDto.getMinPrice() + "," : "",
                            requestDto.getMaxPrice() != null ? "\"lte\": " + requestDto.getMaxPrice() : ""
            ));
        }

        String queryJson = """
        {
          "from": %d,
          "size": %d,
          "query": {
            "script_score": {
              "query": {
                "bool": {
                  "must": [
                    {
                      "multi_match": {
                        "query": "%s",
                        "fields": ["name^3", "description", "tags"]
                      }
                    }
                  ],
                  "filter": [
                    %s
                  ]
                }
              },
              "script": {
                "source": "(cosineSimilarity(params.query_vector, 'embedding') + 1.0) * params.vectorWeight + (_score * params.bm25Weight)",
                "params": {
                  "query_vector": [%s],
                  "vectorWeight": %f,
                  "bm25Weight": %f
                }
              }
            }
          }
        }
        """.formatted(
                requestDto.getPageNumber() * requestDto.getPageSize(),
                requestDto.getPageSize(),
                requestDto.getQuery(),
                filters.toString(),
                vectorJson,
                requestDto.getVectorWeight(),
                requestDto.getBm25Weight()
        );

        try {
            Request request = new Request("POST", "/product/_search");
            request.setJsonEntity(queryJson);

            Response response = restClient.performRequest(request);
            JsonNode hits = objectMapper
                    .readTree(response.getEntity().getContent())
                    .path("hits")
                    .path("hits");

            List<ProductDocument> results = new ArrayList<>();
            for (JsonNode hit : hits) {
                results.add(objectMapper.treeToValue(hit.get("_source"), ProductDocument.class));
            }

            return results;

        } catch (Exception e) {
            log.error("Hybrid search failed", e);
            return Collections.emptyList();
        }
    }

}
