package com.shopfast.cartservice.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Works out which cart a request is addressing.
 *
 * <p>The cart used to be two parallel controllers - {@code /api/v1/cart/**} for
 * signed-in users and {@code /api/v1/cart/guest/**} for everyone else - which made
 * the client responsible for picking the right URL on every call. It picked wrong
 * whenever a stale token made it believe it was logged in, and the guest shopper
 * got a 401 from an endpoint that was never meant for them. Deciding ownership
 * here, from what the request actually proves, removes that choice from the client
 * entirely.
 */
@Component
public class CartIdentityResolver {

    /** Header carrying the browser-generated id for a not-yet-signed-in shopper. */
    public static final String ANON_ID_HEADER = "X-Anon-Id";

    /**
     * @throws ResponseStatusException 400 when the request identifies no cart at
     *         all. Inventing an id server-side instead would hand back a cart the
     *         client cannot address again on the next request, so the basket would
     *         silently empty itself between page loads.
     */
    public CartIdentity resolve(Authentication authentication, HttpServletRequest request) {
        if (CartIdentity.isAuthenticated(authentication)) {
            return CartIdentity.ofUser(authentication.getName());
        }

        String anonId = firstNonBlank(
                request.getHeader(ANON_ID_HEADER),
                // Query parameter kept so links and any client still on the old
                // /guest?anonId=... shape keep working through the transition.
                request.getParameter("anonId"));

        if (anonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Anonymous cart requests must send an " + ANON_ID_HEADER + " header");
        }
        return CartIdentity.ofGuest(requireUuid(anonId));
    }

    /**
     * The id becomes part of a Redis key, so it is constrained to a UUID rather
     * than accepted as free text. Without this an id like {@code ../} or one
     * carrying a delimiter could be shaped to collide with another cart's key.
     */
    private String requireUuid(String anonId) {
        try {
            return UUID.fromString(anonId).toString();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Anonymous cart id must be a UUID");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
