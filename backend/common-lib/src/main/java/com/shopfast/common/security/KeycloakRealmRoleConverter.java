package com.shopfast.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns Keycloak's token structure into Spring Security authorities.
 *
 * <p>Keycloak does not put roles in the "scope" claim that Spring reads by default.
 * Realm roles live under {@code realm_access.roles} and per-client roles under
 * {@code resource_access.<clientId>.roles}. Without this converter every request
 * arrives authenticated but with zero authorities, so every {@code @PreAuthorize}
 * silently fails with 403 - a confusing failure mode worth avoiding.
 *
 * <p>Role names are stored in Keycloak already prefixed with {@code ROLE_}
 * (see realm-export.json), so we do not add a prefix here; doing both would
 * produce {@code ROLE_ROLE_ADMIN}. Any role that is somehow missing the prefix
 * gets it added, so hasRole("ADMIN") keeps working either way.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    /** Client ids whose client-roles should also be mapped, e.g. "shopfast-web". */
    private final List<String> resourceClientIds;

    public KeycloakRealmRoleConverter(List<String> resourceClientIds) {
        this.resourceClientIds = resourceClientIds == null ? List.of() : List.copyOf(resourceClientIds);
    }

    public KeycloakRealmRoleConverter() {
        this(List.of());
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addRoles(authorities, asRoleList(jwt.getClaimAsMap(REALM_ACCESS)));

        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess != null) {
            for (String clientId : resourceClientIds) {
                Object client = resourceAccess.get(clientId);
                if (client instanceof Map<?, ?> clientMap) {
                    addRoles(authorities, asRoleList(clientMap));
                }
            }
        }
        return authorities;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asRoleList(Map<?, ?> claim) {
        if (claim == null) {
            return List.of();
        }
        Object roles = claim.get(ROLES);
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    private static void addRoles(Set<GrantedAuthority> target, List<String> roles) {
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String authority = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
            target.add(new SimpleGrantedAuthority(authority));
        }
    }
}
