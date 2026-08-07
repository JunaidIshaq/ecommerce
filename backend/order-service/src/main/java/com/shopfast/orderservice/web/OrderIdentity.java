package com.shopfast.orderservice.web;

/**
 * Who is buying: a signed-in shopper or a guest browser.
 *
 * @param id    Keycloak subject, or the anonymous browser id for a guest
 * @param guest true when {@code id} is an anonymous browser id
 */
public record OrderIdentity(String id, boolean guest) {

    public static OrderIdentity ofUser(String userId) {
        return new OrderIdentity(userId, false);
    }

    public static OrderIdentity ofGuest(String anonId) {
        return new OrderIdentity(anonId, true);
    }

    public boolean isAuthenticated() {
        return !guest;
    }
}
