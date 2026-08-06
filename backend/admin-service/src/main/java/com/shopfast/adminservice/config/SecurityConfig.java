package com.shopfast.adminservice.config;

import com.shopfast.common.security.KeycloakRealmRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security for the admin APIs.
 *
 * <p>Every endpoint this service exposes is privileged - listing all users, reading
 * every order, adjusting stock, issuing coupons - so the whole {@code /api/v1/admin}
 * tree requires an administrator role. There is no anonymous surface here beyond
 * liveness probes.
 *
 * <p>This previously read {@code .requestMatchers("/api/v1/admin/**").permitAll()}
 * with the role check commented out just above it, which left the entire admin API
 * reachable without any credential at all. The frontend route guard was the only
 * thing standing in the way, and a route guard is a rendering decision, not an
 * access control - anyone could call these endpoints directly with curl.
 *
 * <p>The legacy HS256 filter that used to sit in this chain has been removed. It
 * verified tokens with a secret shared across services, meaning any service could
 * mint an admin token rather than merely verify one. Tokens are now validated only
 * as Keycloak RS256, against the realm's public JWKS.
 */
@EnableWebSecurity
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Roles permitted to reach the admin API.
     *
     * <p>Named without the {@code ROLE_} prefix because {@code hasAnyRole} adds it;
     * the realm stores them as {@code ROLE_ADMIN} / {@code ROLE_SUPER_ADMIN} and
     * {@link KeycloakRealmRoleConverter} maps them to authorities of that exact name.
     */
    private static final String[] ADMIN_ROLES = { "ADMIN", "SUPER_ADMIN" };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // No cookies and no sessions, so CSRF protection guards nothing here
                // and would only reject legitimate bearer-token calls.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight carries no credentials by design.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Only the probe and scrape endpoints, never /actuator/** as a
                        // whole: env and heapdump would hand out the datasource password
                        // and any secret held in memory.
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole(ADMIN_ROLES)
                        // Default deny: a new endpoint is protected until someone
                        // deliberately opens it.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Wired explicitly rather than relying on the converter bean being
                        // picked up implicitly: if it were ever missed, realm roles would
                        // not become authorities and every admin request would 403 - or,
                        // worse under a laxer rule, sail through unauthorised.
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",          // Local Angular
                "https://shopfast.live",          // Production domain
                "https://www.shopfast.live"       // WWW version
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
