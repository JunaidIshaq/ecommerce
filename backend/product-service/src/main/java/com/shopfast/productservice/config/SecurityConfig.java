package com.shopfast.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    /**
     * Storefront reads are public; everything else must prove who it is.
     *
     * <p>Rule order is the whole point here. The previous version listed
     * {@code /api/v1/product/**} as permitAll and ended in
     * {@code anyRequest().permitAll()}, which left the admin listing and every
     * write endpoint open to anonymous callers. Spring Security matches
     * top-to-bottom and stops at the first hit, so the admin and internal
     * patterns have to be declared *before* the public catch-all - a flat
     * "public paths" list cannot express that, because {@code /api/v1/product/*}
     * also matches {@code /api/v1/product/admin}.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight carries no credentials by design.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // Project-wide convention: a /public segment marks an endpoint as
                        // deliberately anonymous, for any method.
                        .requestMatchers("/api/v1/public/**", "/api/v1/*/public/**").permitAll()
                        // Admin surface - declared first so the public GET rule below
                        // cannot swallow it.
                        .requestMatchers("/api/v1/product/admin", "/api/v1/product/admin/**").hasRole("ADMIN")
                        // Service-to-service endpoints: any authenticated caller in the
                        // realm, which in practice means a client-credentials token.
                        .requestMatchers("/api/v1/product/internal/**", "/api/v1/product/*/internal").authenticated()
                        // Anonymous browsing of the catalogue: reads only.
                        .requestMatchers(HttpMethod.GET, "/api/v1/product/**").permitAll()
                        // Writes, and anything newly added, are closed by default.
                        .anyRequest().authenticated()
                )
                // Validate Keycloak RS256 tokens against the realm JWKS. The shared
                // converter is required for hasRole(...) to see Keycloak's realm roles;
                // with the default converter a token carries no authorities and every
                // admin route would 403 regardless of the user.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",          // ✅ Local Angular
                "https://shopfast.live",          // ✅ Production domain
                "https://www.shopfast.live"       // ✅ WWW version
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
