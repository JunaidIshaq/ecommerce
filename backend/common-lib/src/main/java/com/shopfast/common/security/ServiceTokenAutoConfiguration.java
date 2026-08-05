package com.shopfast.common.security;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Wires service-to-service authentication when the credentials are configured.
 *
 * <p>Guarded on {@code shopfast.security.service-client.client-secret} so a service
 * without credentials simply keeps its current behaviour instead of failing to
 * start - important while services are migrated one at a time.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "shopfast.security.service-client", name = "client-secret")
public class ServiceTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceTokenProvider serviceTokenProvider(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${shopfast.security.service-client.client-id:shopfast-services}") String clientId,
            @Value("${shopfast.security.service-client.client-secret}") String clientSecret) {
        return new ServiceTokenProvider(issuerUri, clientId, clientSecret);
    }

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    @ConditionalOnMissingBean(ServiceTokenRelayInterceptor.class)
    public ServiceTokenRelayInterceptor serviceTokenRelayInterceptor(ServiceTokenProvider provider) {
        return new ServiceTokenRelayInterceptor(provider);
    }
}
