package com.shopfast.authservice.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thin wrapper over the Keycloak Admin REST API.
 *
 * <p>Written against plain HTTP rather than the {@code keycloak-admin-client}
 * library on purpose: that library drags in a RESTEasy/JAX-RS stack that clashes
 * with Spring's, and we need perhaps five endpoints.
 *
 * <p>The admin token is cached; minting one per call would triple the request count
 * against Keycloak for no benefit.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);

    private final RestClient http = RestClient.create();
    private final String baseUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile String token;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public KeycloakAdminClient(
            @Value("${keycloak.base-url:http://keycloak:8180}") String baseUrl,
            @Value("${keycloak.realm:shopfast}") String realm,
            @Value("${keycloak.admin.client-id:shopfast-services}") String clientId,
            @Value("${keycloak.admin.client-secret:}") String clientSecret) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // ---------------------------------------------------------------- users

    /**
     * Creates a user and returns its Keycloak id.
     *
     * <p>{@code emailVerified=false} plus a VERIFY_EMAIL required action is
     * deliberate: self-registration must not be able to claim an address the caller
     * does not control, which would otherwise allow account takeover by registering
     * someone else's email before they do.
     */
    public String createUser(String username, String email, String firstName, String lastName,
                             String password, List<String> requiredActions) {
        Map<String, Object> body = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName == null ? "" : firstName,
                "lastName", lastName == null ? "" : lastName,
                "enabled", true,
                "emailVerified", false,
                "requiredActions", requiredActions == null ? List.of("VERIFY_EMAIL") : requiredActions,
                "credentials", password == null ? List.of() : List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false))
        );

        var response = http.post()
                .uri(adminUri("/users"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        // Keycloak returns 201 with the new id only in the Location header.
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a Location header for the created user");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> findByEmail(String email) {
        List<Map<String, Object>> users = http.get()
                .uri(adminUri("/users?email={email}&exact=true"), email)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .retrieve()
                .body(List.class);
        return users == null || users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    /**
     * Stores the ShopFast domain user id on the Keycloak user so it can be minted
     * into tokens. Without it, every service would have to call user-service just to
     * translate a Keycloak subject into a local user row.
     */
    public void setUserIdAttribute(String keycloakId, String shopfastUserId) {
        http.put()
                .uri(adminUri("/users/{id}"), keycloakId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("attributes", Map.of("userId", List.of(shopfastUserId))))
                .retrieve()
                .toBodilessEntity();
    }

    public void sendPasswordReset(String keycloakId) {
        http.put()
                .uri(adminUri("/users/{id}/execute-actions-email"), keycloakId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of("UPDATE_PASSWORD"))
                .retrieve()
                .toBodilessEntity();
    }

    /** Disables rather than deletes: orders and invoices still reference the user. */
    public void disableUser(String keycloakId) {
        http.put()
                .uri(adminUri("/users/{id}"), keycloakId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", false))
                .retrieve()
                .toBodilessEntity();
    }

    /** Ends all of the user's sessions - the "log me out everywhere" action. */
    public void logoutUser(String keycloakId) {
        http.post()
                .uri(adminUri("/users/{id}/logout"), keycloakId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .retrieve()
                .toBodilessEntity();
    }

    // ---------------------------------------------------------------- roles

    @SuppressWarnings("unchecked")
    public void assignRealmRole(String keycloakId, String roleName) {
        Map<String, Object> role = http.get()
                .uri(adminUri("/roles/{role}"), roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .retrieve()
                .body(Map.class);

        if (role == null) {
            throw new IllegalArgumentException("Unknown realm role: " + roleName);
        }

        http.post()
                .uri(adminUri("/users/{id}/role-mappings/realm"), keycloakId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(Map.of("id", role.get("id"), "name", role.get("name"))))
                .retrieve()
                .toBodilessEntity();
    }

    // ---------------------------------------------------------------- internals

    private String adminUri(String suffix) {
        return baseUrl + "/admin/realms/" + realm + suffix;
    }

    private String adminToken() {
        if (token != null && Instant.now().isBefore(tokenExpiry)) {
            return token;
        }
        lock.lock();
        try {
            if (token != null && Instant.now().isBefore(tokenExpiry)) {
                return token;
            }
            refreshAdminToken();
            return token;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshAdminToken() {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "keycloak.admin.client-secret is not configured; auth-service cannot manage users");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        try {
            Map<String, Object> response = http.post()
                    .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("access_token") == null) {
                throw new IllegalStateException("Keycloak returned no admin access_token");
            }
            token = (String) response.get("access_token");
            long expiresIn = response.get("expires_in") instanceof Number n ? n.longValue() : 60L;
            tokenExpiry = Instant.now().plusSeconds(expiresIn).minus(EXPIRY_MARGIN);
        } catch (RestClientResponseException e) {
            // Never log the response body: it can echo back the client secret.
            log.error("Failed to obtain Keycloak admin token: HTTP {}", e.getStatusCode().value());
            throw e;
        }
    }
}
