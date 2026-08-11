package com.shopfast.elasticservice.service.impl;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.service.EmbeddingService;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RestClient restClient;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private ProductSearchServiceImpl searchService;

    @Test
    void semanticSearchReturnsEmptyOnIOException() throws Exception {
        when(embeddingService.embed("shoes")).thenReturn(new float[]{0.1f, 0.2f});
        when(restClient.performRequest(any(Request.class))).thenThrow(new IOException("es down"));

        List<ProductDocument> result = searchService.semanticSearch("shoes", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void semanticSearchParsesHits() throws Exception {
        when(embeddingService.embed("shoes")).thenReturn(new float[]{0.1f, 0.2f});
        String body = """
                {
                  "hits": {
                    "hits": [
                      { "_source": { "id": "1", "name": "Red Shoe", "price": 9.99 } },
                      { "_source": { "id": "2", "name": "Blue Shoe", "price": 19.99 } }
                    ]
                  }
                }
                """;
        Response response = mockResponse(body);
        when(restClient.performRequest(any(Request.class))).thenReturn(response);
        com.fasterxml.jackson.databind.ObjectMapper realMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        when(objectMapper.readTree(any(java.io.InputStream.class)))
                .thenAnswer(inv -> realMapper.readTree((java.io.InputStream) inv.getArgument(0)));
        when(objectMapper.treeToValue(any(JsonNode.class), org.mockito.ArgumentMatchers.eq(com.shopfast.elasticservice.document.ProductDocument.class)))
                .thenAnswer(inv -> realMapper.treeToValue(inv.getArgument(0), com.shopfast.elasticservice.document.ProductDocument.class));

        List<ProductDocument> result = searchService.semanticSearch("shoes", 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Red Shoe");
    }

    private Response mockResponse(String body) throws Exception {
        Response response = org.mockito.Mockito.mock(Response.class);
        HttpEntity entity = org.mockito.Mockito.mock(HttpEntity.class);
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        return response;
    }
}
