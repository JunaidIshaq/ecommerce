package com.shopfast.productservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // Create ObjectMapper with Java Time support
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Create JSON mapper for Elasticsearch client
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);

        // Create low-level REST client
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200)
        ).build();

        // Build transport layer with the custom mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper);

        // Create high-level client
        return new ElasticsearchClient(transport);
    }
}
