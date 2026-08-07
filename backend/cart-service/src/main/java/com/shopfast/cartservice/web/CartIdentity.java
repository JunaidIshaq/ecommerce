package com.shopfast.cartservice.web;

import org.springframework.security.core.Authentication;

/**
 * Who a cart belongs to.
 *
 * <p>A cart is owned either by a signed-in user (keyed on the Keycloak {@code sub})
 * or by an anonymous browser (keyed on a client-generated id). Both are just a
 * string key into Redis; the distinction only decides which key prefix is used,
 * which is why it is worth capturing as one small value rather than duplicating a
 * guest and a user endpoint for every operation.
 */
public record CartIdentity(String id, boolean guest) {

    static CartIdentity ofUser(String userId) {
        return new CartIdentity(userId, false);
    }

    static CartIdentity ofGuest(String anonId) {
        return new CartIdentity(anonId, true);
    }

    /**
     * A token always wins over a client-supplied anonymous id.
     *
     * <p>This ordering is the security-relevant part: if the header could override
     * a verified {@code sub}, any authenticated user could read or edit another
     * shopper's guest cart simply by sending their id, and a signed-in user could
     * be silently writing into an anonymous cart they do not own.
     */
    static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }
}
