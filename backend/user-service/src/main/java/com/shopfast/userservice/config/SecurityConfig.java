package com.shopfast.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * User-service contributes CORS and a password encoder; authentication is the shared
 * {@code ResourceServerAutoConfiguration} chain in common-lib.
 *
 * <p>This class previously declared its own {@code SecurityFilterChain}, which
 * overrode the shared one (it is {@code @ConditionalOnMissingBean}) and brought two
 * problems with it:
 *
 * <ul>
 *   <li>It installed the pre-Keycloak HS256 {@code JwtAuthenticationFilter}, which
 *       cannot verify an RS256 Keycloak token - the same defect that made the cart
 *       return 401 INVALID_TOKEN for every signed-in user.</li>
 *   <li>It permitted {@code /api/v1/user/**} to everyone. That is the whole service:
 *       the user listing, lookup by id, and status changes were all reachable without
 *       a token by anything that could address the container - so the only thing
 *       standing between the internet and a dump of every account was the gateway
 *       remembering to require auth on that prefix.</li>
 * </ul>
 *
 * <p>Now nothing is public by default. Service-to-service callers (auth-service's
 * Feign client for {@code /internal/email}) already send a client-credentials token
 * via the shared relay interceptor, so they authenticate like any other caller.
 *
 * <p>The password encoder stays: rows migrated from before Keycloak still carry a
 * BCrypt hash, even though Keycloak owns credentials for new accounts.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://shopfast.live",
                "https://www.shopfast.live"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
