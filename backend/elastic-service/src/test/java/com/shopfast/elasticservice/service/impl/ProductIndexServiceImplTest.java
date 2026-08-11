package com.shopfast.elasticservice.service.impl;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.elasticsearch.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductIndexServiceImpl}. The embedding/REST-client logic is
 * still a TODO in the implementation, so we verify the repository delegation only.
 */
@ExtendWith(MockitoExtension.class)
class ProductIndexServiceImplTest {

    @Mock
    private ProductSearchRepository repository;

    @Mock
    private com.shopfast.elasticservice.service.EmbeddingService embeddingService;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private ProductIndexServiceImpl service;

    @Test
    void indexDelegatesToRepositoryAndReturnsSavedDocument() {
        ProductDocument document = ProductDocument.builder()
                .id("prod-1")
                .name("Widget")
                .description("A widget")
                .build();
        when(repository.save(any(ProductDocument.class))).thenReturn(document);

        ProductDocument result = service.index(document);

        assertThat(result.getId()).isEqualTo("prod-1");
        verify(repository).save(document);
    }
}
