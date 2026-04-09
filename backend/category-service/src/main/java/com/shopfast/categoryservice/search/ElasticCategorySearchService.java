package com.shopfast.categoryservice.search;

import org.springframework.stereotype.Service;

// import co.elastic.clients.elasticsearch.ElasticsearchClient;
// import co.elastic.clients.elasticsearch._types.ElasticsearchException;
// import co.elastic.clients.elasticsearch._types.SortOrder;
// import co.elastic.clients.elasticsearch.core.CountResponse;
// import co.elastic.clients.elasticsearch.core.SearchResponse;
// import co.elastic.clients.elasticsearch.core.search.Hit;
// import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
// import com.shopfast.categoryservice.dto.SearchResult;
// import com.shopfast.categoryservice.model.Category;
// import org.springframework.stereotype.Service;
// import co.elastic.clients.elasticsearch._types.query_dsl.Query;

// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Objects;
// import java.util.stream.Collectors;

/**
 * Elasticsearch search service - DISABLED for now
 * Uncomment to enable Elasticsearch search functionality
 */
@Service
public class ElasticCategorySearchService {

    // private final ElasticsearchClient client;

    // public ElasticCategorySearchService(ElasticsearchClient client) {
    //     this.client = client;
    // }

    // public void indexCategory(Category category) throws IOException {
    //     client.index(i -> i.index("category").id(String.valueOf(category.getId())).document(category));
    // }

    // public void deleteCategoryFromIndex(String id) {
    //     try {
    //         client.delete(d -> d.index("category").id(id));
    //     } catch (IOException e) {
    //         throw new RuntimeException("Failed to delete category from Elasticsearch: " + id, e);
    //     }
    // }

    // public SearchResult searchCategories(
    //         String keyword,
    //         String sortBy,
    //         String sortOrder,
    //         int page,
    //         int size
    // ) throws IOException {
    //     Query query = Query.of(q -> q
    //             .bool(b -> b
    //                     .should(s -> s.match(m -> m.field("name").query(keyword)))
    //                     .should(s -> s.match(m -> m.field("description").query(keyword)))
    //             )
    //     );

    //     SearchResponse<Category> response = client.search(s -> s
    //             .index("category")
    //             .query(query)
    //             .from(page * size)
    //             .size(size)
    //             .sort(so -> so.field(f -> f.field(sortBy).order(SortOrder.fromString(sortOrder)))),
    //         Category.class
    //     );

    //     List<Category> categories = response.hits().hits().stream()
    //             .map(Hit::source)
    //             .filter(Objects::nonNull)
    //             .collect(Collectors.toList());

    //     return new SearchResult(categories, response.hits().total().value(), page, size);
    // }

    // public long count() {
    //     try {
    //         CountResponse response = client.count(c -> c.index("category"));
    //         return response.count();
    //     } catch (IOException e) {
    //         return 0;
    //     }
    // }

    // public void deleteAllCategories() {
    //     try {
    //         boolean indexExists = client.indices().exists(e -> e.index("category")).value();
    //         if (indexExists) {
    //             client.deleteByQuery(d -> d.index("category").query(q -> q.matchAll(m -> m)));
    //         }
    //     } catch (IOException e) {
    //         throw new RuntimeException("Failed to delete all categories from Elasticsearch", e);
    //     }
    // }

    // Placeholder for health check - returns successfully
    public void healthCheck() {
        // Do nothing - Elasticsearch is disabled
    }

    // Placeholder method to satisfy CategoryService - Elasticsearch is disabled
    public void indexCategory(Object category) {
        // Do nothing - Elasticsearch is disabled
    }
}
