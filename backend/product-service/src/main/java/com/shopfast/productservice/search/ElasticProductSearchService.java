package com.shopfast.productservice.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.shopfast.productservice.dto.SearchResult;
import com.shopfast.productservice.model.Product;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ElasticProductSearchService {

    private final ElasticsearchClient client;

    public ElasticProductSearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public void indexProduct(Product product) throws IOException {
        client.index(i -> i.index("product").id(product.getId()).document(product));
    }

    public void deleteProductFromIndex(String id) {
        try {
            client.delete(d -> d.index("product").id(id));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete product from Elasticsearch: " + id, e);
        }
    }

    public SearchResult searchProducts(
            String keyword,
            List<String> categoryIds,
            Double minPrice,
            Double maxPrice,
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

        // 🏷️ Category filter (supports multiple)
        if (categoryIds != null && !categoryIds.isEmpty()) {
            mustQueries.add(Query.of(q -> q
                    .terms(t -> t
                            .field("categoryId")
                            .terms(v -> v.value(categoryIds.stream().map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))
                    )
            ));
        }
//
//        // 💰 Price range filter
//        if (minPrice != null || maxPrice != null) {
//            mustQueries.add(Query.of(q -> q
//                    .range(r -> r
//                            .field("price")
//                            .gte(minPrice != null ? JsonData.of(minPrice) : null)
//                            .lte(maxPrice != null ? JsonData.of(maxPrice) : null)
//                    )
//            ));
//        }


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
        SearchResponse<Product> response = client.search(s -> s
                        .index("product")
                        .query(finalQuery)
                        .from(from)
                        .size(size)
                        .sort(sort -> sort.field(f -> f.field(sortField).order(order)))
                , Product.class);

        // ✅ Map results
        List<Product> products = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0L;

        return new SearchResult(products, totalHits);
    }

}
