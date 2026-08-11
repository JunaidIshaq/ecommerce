package com.shopfast.orderservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderIdentityResolverTest {

    private final OrderIdentityResolver resolver = new OrderIdentityResolver();

    @Test
    void authenticatedTokenResolvesToUser() {
        var auth = new UsernamePasswordAuthenticationToken("user-sub", null, AuthorityUtils.NO_AUTHORITIES);
        OrderIdentity identity = resolver.resolve(auth, new MockHttpServletRequest());

        assertThat(identity.guest()).isFalse();
        assertThat(identity.id()).isEqualTo("user-sub");
    }

    @Test
    void anonymousTokenFallsBackToGuestHeader() {
        String anonId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OrderIdentityResolver.ANON_ID_HEADER, anonId);
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        OrderIdentity identity = resolver.resolve(anonymous, request);

        assertThat(identity.guest()).isTrue();
        assertThat(identity.id()).isEqualTo(anonId);
    }

    @Test
    void guestHeaderResolvesToGuest() {
        String anonId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OrderIdentityResolver.ANON_ID_HEADER, anonId);

        OrderIdentity identity = resolver.resolve(null, request);

        assertThat(identity.guest()).isTrue();
        assertThat(identity.id()).isEqualTo(anonId);
    }

    @Test
    void missingIdentityIsRejected() {
        assertThatThrownBy(() -> resolver.resolve(null, new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void nonUuidAnonIdIsRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OrderIdentityResolver.ANON_ID_HEADER, "not-a-uuid");

        assertThatThrownBy(() -> resolver.resolve(null, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("UUID");
    }
}
