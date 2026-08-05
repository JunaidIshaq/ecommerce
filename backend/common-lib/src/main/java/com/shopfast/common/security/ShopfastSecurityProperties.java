package com.shopfast.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-service knobs for the shared resource-server configuration.
 *
 * <p>Everything has a safe default: with no configuration at all a service ends up
 * "deny unless authenticated", which is the correct failure direction. Opening a
 * path must be an explicit, reviewable decision in that service's application.yml.
 */
@ConfigurationProperties(prefix = "shopfast.security")
public class ShopfastSecurityProperties {

    /** Turn the shared security auto-configuration off (tests, local spikes). */
    private boolean enabled = true;

    /**
     * Ant patterns that skip authentication entirely. Actuator health/info and the
     * OpenAPI docs are here because probes and Swagger cannot present a token.
     * Note that /actuator/** as a whole is deliberately NOT public - env and
     * heapdump would leak secrets.
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    ));

    /** Client ids whose resource_access roles are also mapped to authorities. */
    private List<String> resourceClientIds = new ArrayList<>(List.of("shopfast-services"));

    /**
     * Expected audience. When set, a token minted for a different client is
     * rejected - this is what stops a token issued for some other application in
     * the same realm from being replayed against these APIs.
     */
    private String expectedAudience;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getResourceClientIds() {
        return resourceClientIds;
    }

    public void setResourceClientIds(List<String> resourceClientIds) {
        this.resourceClientIds = resourceClientIds;
    }

    public String getExpectedAudience() {
        return expectedAudience;
    }

    public void setExpectedAudience(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }
}
