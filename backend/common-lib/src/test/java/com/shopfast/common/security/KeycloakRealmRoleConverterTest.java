package com.shopfast.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every service depends on this converter for authorisation, so the cases that
 * silently produce "authenticated but with no authorities" are the ones worth pinning.
 */
class KeycloakRealmRoleConverterTest {

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("a-subject");
        claims.forEach(builder::claim);
        return builder.build();
    }

    private static List<String> names(Collection<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    @DisplayName("maps realm roles that already carry the ROLE_ prefix without doubling it")
    void mapsPrefixedRealmRoles() {
        var authorities = new KeycloakRealmRoleConverter().convert(
                jwt(Map.of("realm_access", Map.of("roles", List.of("ROLE_ADMIN", "ROLE_USER")))));

        assertThat(names(authorities)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("adds the ROLE_ prefix when Keycloak stores a bare role name")
    void addsMissingPrefix() {
        var authorities = new KeycloakRealmRoleConverter().convert(
                jwt(Map.of("realm_access", Map.of("roles", List.of("SELLER")))));

        assertThat(names(authorities)).containsExactly("ROLE_SELLER");
    }

    @Test
    @DisplayName("maps client roles only for the configured client ids")
    void mapsOnlyConfiguredClients() {
        Map<String, Object> claims = Map.of(
                "realm_access", Map.of("roles", List.of("ROLE_USER")),
                "resource_access", Map.of(
                        "shopfast-web", Map.of("roles", List.of("ROLE_BETA")),
                        // A role from a client we were not told to trust must be ignored;
                        // otherwise a token issued for another application could grant
                        // privileges here.
                        "some-other-app", Map.of("roles", List.of("ROLE_ADMIN"))));

        var authorities = new KeycloakRealmRoleConverter(List.of("shopfast-web")).convert(jwt(claims));

        assertThat(names(authorities)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_BETA");
    }

    @Test
    @DisplayName("returns no authorities rather than throwing when the claims are absent")
    void toleratesMissingClaims() {
        // A client-credentials token has no realm_access at all. Throwing here would
        // turn every service-to-service call into a 500.
        var authorities = new KeycloakRealmRoleConverter(List.of("shopfast-web"))
                .convert(jwt(Map.of("scope", "openid")));

        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("skips null and blank role entries")
    void skipsEmptyRoles() {
        var roles = new java.util.ArrayList<String>();
        roles.add("ROLE_USER");
        roles.add("");
        roles.add(null);

        var authorities = new KeycloakRealmRoleConverter().convert(
                jwt(Map.of("realm_access", Map.of("roles", roles))));

        assertThat(names(authorities)).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("ignores a resource_access entry whose shape is not what we expect")
    void ignoresMalformedResourceAccess() {
        Map<String, Object> claims = Map.of(
                "resource_access", Map.of("shopfast-web", "not-an-object"));

        var authorities = new KeycloakRealmRoleConverter(List.of("shopfast-web")).convert(jwt(claims));

        assertThat(authorities).isEmpty();
    }
}
