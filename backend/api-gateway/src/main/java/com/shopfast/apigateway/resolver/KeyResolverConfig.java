package com.shopfast.apigateway.resolver;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class KeyResolverConfig {

    private static final String UNKNOWN_KEY = "unknown";

    /**
     * Resolves the rate-limit key from the caller's IP address.
     *
     * <p>Behind nginx the socket address may be unresolved (or absent), so
     * {@code InetSocketAddress#getAddress()} can return {@code null}. We therefore
     * prefer the first entry of {@code X-Forwarded-For} and fall back defensively,
     * never throwing, since a failure here turns every request into a 500.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(resolveClientIp(exchange.getRequest()));
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // May be a comma-separated chain; the left-most entry is the original client.
            String client = forwardedFor.split(",")[0].trim();
            if (!client.isEmpty()) {
                return client;
            }
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            if (remoteAddress.getAddress() != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
            if (remoteAddress.getHostString() != null) {
                return remoteAddress.getHostString();
            }
        }

        return UNKNOWN_KEY;
    }
}
