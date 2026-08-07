package com.shopfast.cartservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cart-service only contributes CORS; authentication is the shared
 * {@code ResourceServerAutoConfiguration} chain in common-lib.
 *
 * <p>This class used to declare its own {@link org.springframework.security.web.SecurityFilterChain},
 * which overrode the shared one (it is {@code @ConditionalOnMissingBean}) and installed a
 * hand-rolled HS256 filter from the pre-Keycloak design. That filter rejected every
 * RS256 Keycloak token with 401 INVALID_TOKEN, so no logged-in user could use the cart.
 * The same chain also had {@code /api/v1/cart/**} as blanket permitAll, which would have
 * left the authenticated and internal cart endpoints open once the filter was removed.
 *
 * <p>Anonymous guest-cart paths are declared as {@code shopfast.security.public-paths}
 * in application.yml, so they stay open without opening the whole service.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://shopfast.live",
                "https://www.shopfast.live"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // X-Anon-Id identifies a guest basket. It is a custom header, so the browser
        // will not send it unless the preflight response names it here - omitting it
        // fails anonymous cart calls in the browser while curl still works.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-Anon-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
