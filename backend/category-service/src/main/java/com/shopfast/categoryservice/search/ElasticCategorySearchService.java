package com.shopfast.categoryservice.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.shopfast.categoryservice.dto.SearchResult;
import com.shopfast.categoryservice.model.Category;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ElasticCategorySearchService {

    private final ElasticsearchClient client;

    public ElasticCategorySearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public void indexCategory(Category category) throws IOException {
        client.index(i -> i.index("category").id(String.valueOf(category.getId())).document(category));
    }

    public void deleteCategoryFromIndex(String id) {
        try {
            client.delete(d -> d.index("category").id(id));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete category from Elasticsearch: " + id, e);
        }
    }

    public SearchResult searchCategories(
            String keyword,
            String sortBy,
            String sortOrder,
            int page,
            int size
    ) throws IOException {

        int from = Math.max(0, page) * size;

        List<Query> mustQueries = new ArrayList<>();

        // 🔍 Full-text keyword search (fuzzy)
        if (keyword != null && !keyword.isEmpty()) {
            mustQueries.add(Query.of(q -> q
                    .multiMatch(mm -> mm
                            .fields("title^3", "description")
                            .query(keyword)
                            .fuzziness("AUTO")
                    )
            ));
        }

        // 🧠 Combine all filters using bool query
        Query finalQuery = mustQueries.isEmpty()
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(b -> b.must(mustQueries)));

        // 📊 Sorting logic
        SortOrder order = "asc".equalsIgnoreCase(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
        String sortField = switch (sortBy != null ? sortBy : "createdAt") {
            case "price" -> "price";
            case "title" -> "title.keyword";
            default -> "createdAt";
        };

        // 🔎 Execute search
        SearchResponse<Category> response = client.search(s -> s
                        .index("category")
                        .query(finalQuery)
                        .from(from)
                        .size(size)
                        .sort(sort -> sort.field(f -> f.field(sortField).order(order)))
                , Category.class);

        // ✅ Map results
        List<Category> categories = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0L;

        return new SearchResult(categories, totalHits);
    }

}
