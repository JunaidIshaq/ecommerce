package com.shopfast.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fetches and caches a client-credentials access token for internal calls.
 *
 * <p>Background: today a service-to-service call carries whatever token the end
 * user happened to send, or nothing at all - so the callee ends up trusting the
 * network. With a dedicated {@code shopfast-services} token, internal calls are
 * authenticated in their own right and can be authorised and audited separately
 * from user traffic.
 *
 * <p>The token is cached until shortly before expiry. Re-fetching per request
 * would put Keycloak on the hot path of every internal hop and make it a single
 * point of failure for all traffic, not just logins.
 */
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    /** Renew this long before actual expiry to absorb clock skew and request latency. */
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(String issuerUri, String clientId, String clientSecret) {
        this.restClient = RestClient.create();
        this.tokenEndpoint = issuerUri.replaceAll("/$", "") + "/protocol/openid-connect/token";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getToken() {
        if (isValid()) {
            return cachedToken;
        }
        lock.lock();
        try {
            // Another thread may have refreshed while we waited for the lock.
            if (isValid()) {
                return cachedToken;
            }
            refresh();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private boolean isValid() {
        return cachedToken != null && Instant.now().isBefore(expiresAt);
    }

    @SuppressWarnings("unchecked")
    private void refresh() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> response = restClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Keycloak returned no access_token for client " + clientId);
        }

        cachedToken = (String) response.get("access_token");
        long expiresIn = response.get("expires_in") instanceof Number n ? n.longValue() : 60L;
        expiresAt = Instant.now().plusSeconds(expiresIn).minus(EXPIRY_MARGIN);
        log.debug("Obtained service token for {}, valid for {}s", clientId, expiresIn);
    }

    /** Force the next call to fetch a fresh token, e.g. after a 401 from a peer. */
    public void invalidate() {
        cachedToken = null;
        expiresAt = Instant.EPOCH;
    }
}
