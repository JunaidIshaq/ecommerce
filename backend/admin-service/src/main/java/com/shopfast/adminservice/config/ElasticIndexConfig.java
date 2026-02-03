package com.shopfast.adminservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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


//    @PostConstruct
//    public void createProductIndexIfNotExists() {
//        try {
//            boolean exists = client.indices().exists(e -> e.index("products")).value();
//            if (!exists) {
//                log.info("Creating Elasticsearch index: products");
//
//                CreateIndexResponse response = client.indices().create(c -> c
//                        .index("products")
//                        .settings(s -> s
//                                .analysis(a -> a
//                                        .analyzer("custom_text_analyzer", analyzer -> analyzer
//                                                .custom(ca -> ca
//                                                        .tokenizer("standard")
//                                                        .filter("lowercase", "asciifolding", "porter_stem")
//                                                )
//                                        )
//                                )
//                        )
//                        .mappings(m -> m
//                                .properties("id", p -> p.keyword(k -> k))
//                                .properties("name", p -> p.text(t -> t
//                                        .analyzer("custom_text_analyzer")
//                                        .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))
//                                ))
//                                .properties("description", p -> p.text(t -> t.analyzer("custom_text_analyzer")))
//                                .properties("categoryId", p -> p.keyword(k -> k))
//                                .properties("price", p -> p.double_(d -> d))
//                                .properties("stock", p -> p.integer(i -> i))
//                                .properties("images", p -> p.keyword(k -> k))
//                                .properties("createdAt", p -> p.date(d -> d))
//                                .properties("updatedAt", p -> p.date(d -> d))
//                                .properties("createdBy", p ->  p.keyword(d -> d))
//                                .properties("updatedBy", p ->  p.keyword(d -> d))
//                        )
//                );
//
//                if (response.acknowledged()) {
//                    log.info("Elasticsearch index 'products' created successfully");
//                }
//            } else {
//                log.info("Elasticsearch index 'products' already exists");
//            }
//        } catch (IOException e) {
//            log.error("Error creating Elasticsearch index mapping", e);
//        }
//    }
}
