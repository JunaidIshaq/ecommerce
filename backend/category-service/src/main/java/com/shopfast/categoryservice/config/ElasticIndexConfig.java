package com.shopfast.categoryservice.config;

// import co.elastic.clients.elasticsearch.ElasticsearchClient;
// import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;

/**
 * Elasticsearch index configuration - DISABLED for now
 * Uncomment to enable Elasticsearch index management
 */
// @Slf4j
// @Component
// @RequiredArgsConstructor
public class ElasticIndexConfig {

    // private final ElasticsearchClient client;

    // public void createCategoryIndexIfNotExists() {
    //     try {
    //         boolean indexExists = client.indices().exists(e -> e.index("category")).value();
    //         if (!indexExists) {
    //             client.indices().create(c -> c
    //                 .index("category")
    //                 .mappings(m -> m
    //                     .properties("id", p -> p.keyword(k -> k))
    //                     .properties("name", p -> p.text(t -> t.analyzer("standard")))
    //                     .properties("description", p -> p.text(t -> t.analyzer("standard")))
    //                     .properties("slug", p -> p.keyword(k -> k))
    //                     .properties("parentId", p -> p.keyword(k -> k))
    //                 )
    //             );
    //             log.info("✅ Created Elasticsearch index: category");
    //         } else {
    //             log.info("ℹ️ Elasticsearch index already exists: category");
    //         }
    //     } catch (Exception e) {
    //         log.error("❌ Failed to create Elasticsearch index: {}", e.getMessage());
    //     }
    // }

    // public void resetCategoryIndex() {
    //     try {
    //         boolean indexExists = client.indices().exists(e -> e.index("category")).value();
    //         if (indexExists) {
    //             client.indices().delete(d -> d.index("category"));
    //             log.info("✅ Deleted Elasticsearch index: category");
    //         }
    //     } catch (Exception e) {
    //         log.warn("⚠️ Failed to delete Elasticsearch index: {}", e.getMessage());
    //     }
    // }
}
