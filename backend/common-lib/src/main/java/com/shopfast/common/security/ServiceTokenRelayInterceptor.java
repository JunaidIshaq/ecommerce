package com.shopfast.common.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Puts an Authorization header on every outgoing Feign call.
 *
 * <p>Two modes, in priority order:
 * <ol>
 *   <li><b>Relay</b> the caller's token when there is one, so the downstream service
 *       sees the real end user and can enforce per-user rules (e.g. "you may only
 *       read your own orders"). Swapping in a service token here would silently
 *       escalate privileges.</li>
 *   <li><b>Client credentials</b> when there is no user context - schedulers, Kafka
 *       consumers, startup tasks - so the call is still authenticated rather than
 *       relying on the network being private.</li>
 * </ol>
 */
public class ServiceTokenRelayInterceptor implements RequestInterceptor {

    private static final String BEARER = "Bearer ";

    private final ServiceTokenProvider serviceTokenProvider;

    public ServiceTokenRelayInterceptor(ServiceTokenProvider serviceTokenProvider) {
        this.serviceTokenProvider = serviceTokenProvider;
    }

    @Override
    public void apply(RequestTemplate template) {
        // Never overwrite a header a caller set deliberately.
        if (template.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
            return;
        }

        String token = currentUserToken();
        if (token == null) {
            token = serviceTokenProvider.getToken();
        }
        template.header(HttpHeaders.AUTHORIZATION, BEARER + token);
    }

    private String currentUserToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        return null;
    }
}
