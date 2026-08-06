package com.shopfast.apigateway.security;

import com.shopfast.common.security.KeycloakRealmRoleConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway authentication.
 *
 * <p>Replaces the hand-rolled {@code JwtAuthFilter}, which had three problems worth
 * naming so they do not come back:
 * <ul>
 *   <li>Its allow-list used {@code path.startsWith("/api/v1/auth/**")}. That is a
 *       literal string comparison, not a pattern, so it never matched and the login
 *       endpoints were only reachable by accident of routing.</li>
 *   <li>It verified tokens with a symmetric secret shared by every service, so any
 *       service could mint tokens, not merely verify them.</li>
 *   <li>It validated the signature but produced no {@code Authentication}, so
 *       nothing downstream of the gateway could authorise on roles.</li>
 * </ul>
 *
 * <p>The gateway is the first line, not the only one: services validate tokens
 * again themselves, so a request that reaches a pod directly is still checked.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /** Infrastructure endpoints and the pre-token auth calls. */
    private static final String[] PUBLIC_PATHS = {
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            // Reachable before a token exists. Listed individually rather than as
            // /api/v1/auth/** so that logout-all and validate - which act on an
            // existing session - are not accidentally opened too.
            "/api/v1/auth/register",
            "/api/v1/auth/password-reset",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
    };

    /**
     * Project-wide convention: a {@code /public} path segment marks an endpoint as
     * deliberately anonymous, so opening one is a visible decision in the URL rather
     * than a hidden entry in a config file.
     *
     * <p>Enumerated rather than written as {@code /**}{@code /public/**} because
     * WebFlux matches with {@link org.springframework.web.util.pattern.PathPattern},
     * which only permits {@code **} as the final segment.
     */
    private static final String[] PUBLIC_CONVENTION_PATHS = {
            "/api/v1/public/**",
            "/api/v1/*/public/**",
            "/api/public/**",
    };

    /**
     * Anonymous storefront traffic. A shopper browses the catalogue and fills a
     * guest basket before authenticating, so these must pass the gateway without
     * a token - otherwise routing public traffic through the gateway would turn
     * the whole shop into a login wall.
     *
     * <p>Reads only: the matching write endpoints stay authenticated.
     */
    private static final String[] PUBLIC_GET_PATHS = {
            "/api/v1/product/**",
            "/api/v1/category/**",
    };

    private static final String[] PUBLIC_CART_PATHS = {
            "/api/v1/cart/guest",
            "/api/v1/cart/guest/**",
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveJwtDecoder jwtDecoder) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRealmRoleConverter(List.of("shopfast-web", "shopfast-mobile", "shopfast-admin")));
        converter.setPrincipalClaimName("sub");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // CORS preflight carries no credentials by design.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers(PUBLIC_CONVENTION_PATHS).permitAll()
                        // Every administrative surface, gated on the role rather than
                        // merely on being logged in. `authenticated()` here would let any
                        // customer with a valid token list all users and edit stock, since
                        // a token proves who you are, not what you may do.
                        //
                        // admin-service enforces the same rule itself; this is the outer
                        // of two checks, so a request that bypasses the gateway and hits
                        // the pod directly is still rejected.
                        .pathMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // Declared before the catch-all: the admin listing lives under
                        // the same /api/v1/product prefix as the public catalogue, so
                        // it has to be claimed first or the GET rule would open it.
                        .pathMatchers("/api/v1/product/admin/**", "/api/v1/category/admin/**")
                            .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .pathMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                        .pathMatchers(PUBLIC_CART_PATHS).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                        new ReactiveJwtAuthenticationConverterAdapter(converter))))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${keycloak.issuer-uri}") String issuerUri,
            @Value("${jwt.secret:}") String legacySecret) {

        ReactiveJwtDecoder keycloak = ReactiveJwtDecoders.fromIssuerLocation(issuerUri);

        if (!StringUtils.hasText(legacySecret)) {
            // Migration finished (or never started): only Keycloak tokens are accepted.
            return keycloak;
        }

        ReactiveJwtDecoder legacy = NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(legacySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        return new DualReactiveJwtDecoder(keycloak, legacy);
    }
}
