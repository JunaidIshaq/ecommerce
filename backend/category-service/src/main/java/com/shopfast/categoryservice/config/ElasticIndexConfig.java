package com.shopfast.categoryservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ElasticIndexConfig {

    private final ElasticsearchClient client;

    public ElasticIndexConfig(ElasticsearchClient client) {
        this.client = client;
    }

//    @PostConstruct
//    public void resetProductIndex() {
//        try {
//            boolean exists = client.indices().exists(e -> e.index("products")).value();
//            if (exists) {
//                client.indices().delete(d -> d.index("products"));
//                log.info("Old Elasticsearch index 'products' deleted successfully");
//            }
//        } catch (IOException e) {
//            log.error("Failed to delete old Elasticsearch index", e);
//        }
//    }


    @PostConstruct
    public void createCategoryIndexIfNotExists() {
        try {
            boolean exists = client.indices().exists(e -> e.index("category")).value();
            if (!exists) {
                log.info("Creating Elasticsearch index: category");

                CreateIndexResponse response = client.indices().create(c -> c
                        .index("category")
                        .settings(s -> s
                                .analysis(a -> a
                                        .analyzer("custom_text_analyzer", analyzer -> analyzer
                                                .custom(ca -> ca
                                                        .tokenizer("standard")
                                                        .filter("lowercase", "asciifolding", "porter_stem")
                                                )
                                        )
                                )
                        )
                        .mappings(m -> m
                                .properties("id", p -> p.keyword(k -> k))
                                .properties("name", p -> p.text(t -> t
                                        .analyzer("custom_text_analyzer")
                                        .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))
                                ))
                                .properties("description", p -> p.text(t -> t.analyzer("custom_text_analyzer")))
                                .properties("parentId", p -> p.keyword(k -> k))
                                .properties("subCategoryIds", p -> p.keyword(d -> d))
                                .properties("createdAt", p -> p.date(d -> d))
                                .properties("updatedAt", p -> p.date(d -> d))
                                .properties("createdBy", p ->  p.keyword(d -> d))
                                .properties("updatedBy", p ->  p.keyword(d -> d))
                        )
                );

                if (response.acknowledged()) {
                    log.info("Elasticsearch index 'category' created successfully");
                }
            } else {
                log.info("Elasticsearch index 'category' already exists");
            }
        } catch (IOException e) {
            log.error("Error creating Elasticsearch index mapping", e);
        }
    }
}
