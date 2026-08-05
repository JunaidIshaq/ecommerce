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

    /** Everything a caller must reach *before* they hold a token. */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
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
