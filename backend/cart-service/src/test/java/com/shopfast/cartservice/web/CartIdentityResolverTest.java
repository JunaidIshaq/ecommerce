package com.shopfast.cartservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartIdentityResolverTest {

    private final CartIdentityResolver resolver = new CartIdentityResolver();

    @Test
    void anonymousRequestWithHeaderResolvesToGuestCart() {
        String anonId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CartIdentityResolver.ANON_ID_HEADER, anonId);

        CartIdentity identity = resolver.resolve(null, request);

        assertThat(identity.guest()).isTrue();
        assertThat(identity.id()).isEqualTo(anonId);
    }

    /** The bug this whole change exists to prevent: a guest getting rejected. */
    @Test
    void springAnonymousTokenIsTreatedAsGuestNotAsAUser() {
        String anonId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CartIdentityResolver.ANON_ID_HEADER, anonId);
        var anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        CartIdentity identity = resolver.resolve(anonymous, request);

        assertThat(identity.guest()).isTrue();
        assertThat(identity.id()).isEqualTo(anonId);
    }

    @Test
    void tokenWinsOverASuppliedAnonId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CartIdentityResolver.ANON_ID_HEADER, UUID.randomUUID().toString());
        var user = new UsernamePasswordAuthenticationToken("user-sub", null, AuthorityUtils.NO_AUTHORITIES);

        CartIdentity identity = resolver.resolve(user, request);

        assertThat(identity.guest()).isFalse();
        assertThat(identity.id()).isEqualTo("user-sub");
    }

    @Test
    void legacyQueryParameterStillResolves() {
        String anonId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("anonId", anonId);

        assertThat(resolver.resolve(null, request).id()).isEqualTo(anonId);
    }

    @Test
    void requestIdentifyingNoCartIsRejected() {
        assertThatThrownBy(() -> resolver.resolve(null, new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void nonUuidAnonIdIsRejectedSoItCannotBeShapedIntoAnotherCartsKey() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CartIdentityResolver.ANON_ID_HEADER, "cart:user:someone-else");

        assertThatThrownBy(() -> resolver.resolve(null, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }
}
