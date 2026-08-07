package com.shopfast.orderservice.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Works out who is checking out.
 *
 * <p>Checkout previously took the buyer from a client-supplied {@code X-User-Id}
 * header, which meant anyone could place - and then read - an order in someone
 * else's name simply by editing a header. The buyer now comes from the token when
 * there is one; the anonymous browser id is only ever trusted to address a guest
 * basket, never to speak for an account.
 */
@Component
public class OrderIdentityResolver {

    /** Header carrying the browser-generated id for a not-yet-signed-in shopper. */
    public static final String ANON_ID_HEADER = "X-Anon-Id";

    /**
     * @throws ResponseStatusException 400 when the request identifies no buyer.
     *         There is nothing sensible to invent here: a guest order keyed to a
     *         server-generated id could never be looked up again by its owner.
     */
    public OrderIdentity resolve(Authentication authentication, HttpServletRequest request) {
        if (isAuthenticated(authentication)) {
            return OrderIdentity.ofUser(authentication.getName());
        }

        String anonId = request.getHeader(ANON_ID_HEADER);
        if (anonId == null || anonId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Guest checkout must send an " + ANON_ID_HEADER + " header");
        }
        return OrderIdentity.ofGuest(requireUuid(anonId.trim()));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * The id is forwarded to cart-service where it becomes part of a Redis key, and
     * it is stored as the order's owner. Constraining it to a UUID keeps a crafted
     * value from being shaped to collide with another shopper's basket.
     */
    private String requireUuid(String anonId) {
        try {
            return UUID.fromString(anonId).toString();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Anonymous id must be a UUID");
        }
    }
}
