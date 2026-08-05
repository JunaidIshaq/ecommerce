package com.shopfast.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * One resource-server configuration, shared by every ShopFast service.
 *
 * <p>Duplicating this per service is how services drift apart - one ends up with
 * CSRF on, another forgets the role converter, a third leaves /actuator/env open.
 * Keeping it here means a security fix lands everywhere with one release.
 *
 * <p>Activated by setting {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
 * A service can opt out with {@code shopfast.security.enabled=false}, and can always
 * define its own {@link SecurityFilterChain} bean to override this one entirely.
 *
 * <p>Restricted to servlet applications: {@link HttpSecurity} is the servlet-stack
 * builder and does not exist in a reactive context, so applying this to the WebFlux
 * api-gateway would fail its startup. The gateway configures its own reactive chain.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ SecurityFilterChain.class, JwtAuthenticationConverter.class })
@ConditionalOnProperty(prefix = "shopfast.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ShopfastSecurityProperties.class)
@EnableMethodSecurity
public class ResourceServerAutoConfiguration {

    /**
     * Maps Keycloak realm/client roles onto Spring authorities. Without it every
     * {@code @PreAuthorize("hasRole(...)")} in the platform would fail closed.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(ShopfastSecurityProperties properties) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRealmRoleConverter(properties.getResourceClientIds()));
        // Principal name = Keycloak user id (sub), which is stable across email and
        // username changes, unlike the username.
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * Decoder that verifies tokens against the realm's public JWKS, so key rotation
     * is picked up automatically instead of requiring a redeploy.
     *
     * <p>Two ways to find those keys:
     *
     * <ul>
     *   <li>{@code issuer-uri} alone - the keys are discovered from the issuer's
     *       well-known document. Simplest, and correct when the issuer URL is
     *       reachable from inside the network.</li>
     *   <li>{@code issuer-uri} plus {@code jwk-set-uri} - keys are fetched from the
     *       explicit URL while the {@code iss} claim is still validated against the
     *       issuer.</li>
     * </ul>
     *
     * <p>The second form exists because the issuer must be the URL the *browser*
     * uses (it is baked into every token Keycloak mints, and a mismatch fails
     * validation), but that public URL is often not usable from inside the container
     * network - it would mean hairpinning out to the public IP and back through the
     * TLS terminator just to read a public key, making startup depend on the reverse
     * proxy. Pointing jwk-set-uri at the internal address avoids that without
     * weakening anything: the issuer is still checked, and JWKS is public data.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "issuer-uri")
    public NimbusJwtDecoder jwtDecoder(ShopfastSecurityProperties properties,
                                       org.springframework.core.env.Environment environment) {
        String issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        String jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");

        NimbusJwtDecoder decoder = (jwkSetUri != null && !jwkSetUri.isBlank())
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                : (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        // Note this is applied in both branches: skipping the issuer check when
        // jwk-set-uri is set would accept a token minted by any realm on that server.
        validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));
        if (properties.getExpectedAudience() != null && !properties.getExpectedAudience().isBlank()) {
            validators.add(new AudienceValidator(properties.getExpectedAudience()));
        }
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain shopfastSecurityFilterChain(HttpSecurity http,
                                                           ShopfastSecurityProperties properties,
                                                           JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        String[] publicPaths = properties.getPublicPaths().toArray(String[]::new);

        http
                // No cookies, no sessions - CSRF protection guards cookie-based auth and
                // would only break these stateless bearer-token APIs.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Browsers send an unauthenticated preflight; blocking it breaks CORS.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(publicPaths).permitAll()
                        // Default deny: a newly added endpoint is protected until someone
                        // deliberately opens it.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                // Bearer tokens are the only accepted credential; leaving basic auth or
                // a login form enabled would create a second, unmonitored way in.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}
