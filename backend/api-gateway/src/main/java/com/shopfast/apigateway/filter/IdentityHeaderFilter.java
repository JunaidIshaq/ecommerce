package com.shopfast.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Derives the caller's identity headers from the verified access token.
 *
 * <p>Most downstream controllers were written against a {@code userId} (or
 * {@code X-User-Id}) request header that the pre-Keycloak gateway used to add.
 * That filter was removed during the migration and nothing replaced it, so every
 * such endpoint began failing with
 * {@code Required request header 'userId' ... is not present}.
 *
 * <p>Re-adding the header at the gateway fixes those endpoints without editing
 * twenty controllers, but the more important point is the stripping below.
 * A header is just text a client can type. Until now nothing stopped a caller
 * from sending {@code userId: <somebody-else>} and being believed - and because
 * services are reachable from one another inside the network, that is a real
 * horizontal-privilege-escalation path, not a theoretical one. So the inbound
 * copies are removed unconditionally, before the authenticated values are set;
 * a client cannot contribute to these headers, only the token can.
 *
 * <p>Anonymous requests (public catalogue, guest cart) simply arrive with the
 * headers absent, which is what those endpoints already expect.
 *
 * <p>Note this is defence in depth, not the boundary itself: services validate
 * the bearer token independently, and new code should prefer reading the
 * identity from the token via {@code @AuthenticationPrincipal} rather than
 * trusting a header.
 */
@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(IdentityHeaderFilter.class);

    /**
     * Every spelling of the identity headers currently in use. The duplication is
     * historical - different services were written at different times - and the
     * list is deliberately generous: anything a downstream service might trust
     * must be scrubbed here, or scrubbing the rest achieves nothing.
     */
    private static final List<String> IDENTITY_HEADERS = List.of(
            "userId",
            "user_id",
            "X-User-Id",
            "X-User-Email",
            "X-User-Roles",
            "X-User-Name");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(token -> withIdentity(exchange, token))
                // No token: strip only. An anonymous request must not be able to
                // smuggle identity headers past the gateway either.
                .defaultIfEmpty(withoutIdentity(exchange))
                .flatMap(chain::filter);
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, JwtAuthenticationToken token) {
        Jwt jwt = token.getToken();

        // "sub" is Keycloak's stable, immutable user id. Usernames and emails can
        // be changed by the user, so they must never be used as a key.
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String roles = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    IDENTITY_HEADERS.forEach(headers::remove);

                    if (StringUtils.hasText(userId)) {
                        // Both spellings are populated because both are in use
                        // downstream; see IDENTITY_HEADERS.
                        headers.add("userId", userId);
                        headers.add("user_id", userId);
                        headers.add("X-User-Id", userId);
                    }
                    if (StringUtils.hasText(email)) {
                        headers.add("X-User-Email", email);
                    }
                    if (StringUtils.hasText(username)) {
                        headers.add("X-User-Name", username);
                    }
                    if (StringUtils.hasText(roles)) {
                        headers.add("X-User-Roles", roles);
                    }
                })
                .build();

        if (log.isDebugEnabled()) {
            log.debug("Injected identity headers for subject {} on {} {}",
                    userId, exchange.getRequest().getMethod(), exchange.getRequest().getPath());
        }

        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange withoutIdentity(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        return exchange.mutate().request(request).build();
    }

    /**
     * Runs before the routing filters so the mutated request is what actually gets
     * forwarded. {@code HIGHEST_PRECEDENCE + 100} leaves room below for anything
     * that must observe the raw request first, while still sitting well ahead of
     * {@code NettyRoutingFilter}.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
