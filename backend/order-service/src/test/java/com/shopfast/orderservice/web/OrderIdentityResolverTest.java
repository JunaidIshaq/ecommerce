package com.shopfast.orderservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderIdentityResolverTest {

    private final OrderIdentityResolver resolver = new OrderIdentityResolver();

    @Test
    void takesTheBuyerFromTheTokenWhenThereIsOne() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "9f1c6b6e-0000-4000-8000-000000000001", "n/a", AuthorityUtils.NO_AUTHORITIES);

        OrderIdentity identity = resolver.resolve(auth, new MockHttpServletRequest());

        assertThat(identity.guest()).isFalse();
        assertThat(identity.id()).isEqualTo("9f1c6b6e-0000-4000-8000-000000000001");
    }

    /** A header must never be able to override a verified token. */
    @Test
    void ignoresTheAnonymousHeaderWhenSignedIn() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "real-user", "n/a", AuthorityUtils.NO_AUTHORITIES);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Anon-Id", UUID.randomUUID().toString());

        assertThat(resolver.resolve(auth, request).id()).isEqualTo("real-user");
    }

    @Test
    void fallsBackToTheAnonymousIdForGuestCheckout() {
        String anonId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Anon-Id", anonId);

        OrderIdentity identity = resolver.resolve(null, request);

        assertThat(identity.guest()).isTrue();
        assertThat(identity.id()).isEqualTo(anonId);
    }

    @Test
    void treatsSpringsAnonymousTokenAsNotSignedIn() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        String anonId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Anon-Id", anonId);

        assertThat(resolver.resolve(anonymous, request).guest()).isTrue();
    }

    @Test
    void rejectsARequestThatIdentifiesNoBuyer() {
        assertThatThrownBy(() -> resolver.resolve(null, new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** The id reaches a Redis key downstream, so free text is not accepted. */
    @Test
    void rejectsAnAnonymousIdThatIsNotAUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Anon-Id", "../other-cart");

        assertThatThrownBy(() -> resolver.resolve(null, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
