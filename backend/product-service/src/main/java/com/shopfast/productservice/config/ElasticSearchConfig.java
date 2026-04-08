package com.shopfast.productservice.config;

// import co.elastic.clients.elasticsearch.ElasticsearchClient;
// import co.elastic.clients.json.jackson.JacksonJsonpMapper;
// import co.elastic.clients.transport.ElasticsearchTransport;
// import co.elastic.clients.transport.rest_client.RestClientTransport;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import org.apache.http.HttpHost;
// import org.elasticsearch.client.RestClient;
// import org.springframework.beans.factory.annotation.Value();
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch configuration - DISABLED for now
 * Uncomment to enable Elasticsearch integration
 */
// @Configuration
public class ElasticSearchConfig {

    // @Value("${elastic.host.name}")
    // private String hostName;

    // @Bean
    // public ElasticsearchClient elasticsearchClient() {
    //     ObjectMapper mapper = new ObjectMapper();
    //     mapper.registerModule(new JavaTimeModule());
    //     JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);
    //     RestClient restClient = RestClient.builder(new HttpHost(hostName, 9200)).build();
    //     ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper);
    //     return new ElasticsearchClient(transport);
    // }
}
