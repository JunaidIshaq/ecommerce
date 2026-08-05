package com.shopfast.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read the caller's identity from the validated token.
 *
 * <p>Controllers must never take a user id from a request body or a header - the
 * only trustworthy source is the signed token. This helper keeps that rule easy to
 * follow so nobody reintroduces "pass userId as a query param" authorisation.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    private static Optional<Jwt> jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.of(token.getToken());
        }
        return Optional.empty();
    }

    /** Keycloak user id ({@code sub}) - the stable cross-service identifier. */
    public static Optional<String> keycloakId() {
        return jwt().map(Jwt::getSubject);
    }

    /**
     * ShopFast domain user id, injected as the {@code userId} claim by the
     * shopfast-profile client scope. Absent for service-account tokens, which have
     * no human behind them.
     */
    public static Optional<String> userId() {
        return jwt().map(j -> j.getClaimAsString("userId"));
    }

    public static Optional<String> email() {
        return jwt().map(j -> j.getClaimAsString("email"));
    }

    public static Optional<String> preferredUsername() {
        return jwt().map(j -> j.getClaimAsString("preferred_username"));
    }

    public static Set<String> roles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public static boolean hasRole(String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return roles().contains(authority);
    }
}
