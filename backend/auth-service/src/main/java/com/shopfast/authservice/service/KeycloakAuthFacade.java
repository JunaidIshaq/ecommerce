package com.shopfast.authservice.service;

import com.shopfast.authservice.client.UserClient;
import com.shopfast.authservice.dto.RegisterRequestDto;
import com.shopfast.authservice.dto.UserInternalDto;
import com.shopfast.authservice.keycloak.KeycloakAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Registration and account-lifecycle operations, now backed by Keycloak.
 *
 * <p>auth-service no longer issues tokens. Clients obtain them directly from Keycloak
 * with Authorization Code + PKCE; what remains here is the glue that keeps the
 * Keycloak user and the ShopFast profile in step.
 */
@Service
public class KeycloakAuthFacade {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthFacade.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeycloakAdminClient keycloak;
    private final UserClient userClient;

    public KeycloakAuthFacade(KeycloakAdminClient keycloak, UserClient userClient) {
        this.keycloak = keycloak;
        this.userClient = userClient;
    }

    /**
     * Creates the Keycloak identity, then the ShopFast profile, then links the two.
     *
     * <p>Keycloak first, deliberately. If the profile call fails we can disable the
     * half-created identity so nobody can authenticate with it; the reverse order
     * would leave a profile that is reachable by a later registration attempt for
     * the same email but owned by nobody.
     */
    public RegistrationResult register(RegisterRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();

        // Do not tell an anonymous caller whether an email is already registered -
        // that turns this endpoint into an account-enumeration oracle. Callers get
        // the same neutral response either way; the real user gets an email.
        if (keycloak.findByEmail(email).isPresent()) {
            log.info("Registration attempted for an already-registered email");
            return new RegistrationResult(null, null, false);
        }

        String keycloakId = keycloak.createUser(
                email, email, request.getFirstName(), request.getLastName(),
                request.getPassword(), List.of("VERIFY_EMAIL"));

        try {
            keycloak.assignRealmRole(keycloakId, "ROLE_USER");

            // The profile row must not hold a usable credential: Keycloak is the only
            // password authority now. A random value satisfies the legacy NOT NULL
            // column without ever being checkable.
            UserInternalDto profile = userClient.register(new RegisterRequestDto(
                    email, randomPlaceholderPassword(), request.getFirstName(), request.getLastName()));

            keycloak.setUserIdAttribute(keycloakId, profile.getId().toString());
            return new RegistrationResult(keycloakId, profile.getId().toString(), true);
        } catch (RuntimeException e) {
            log.error("Profile sync failed after creating Keycloak user {}; disabling it", keycloakId, e);
            try {
                keycloak.disableUser(keycloakId);
            } catch (RuntimeException cleanupFailure) {
                // Surfacing this matters: a stranded enabled identity with no profile
                // will fail in confusing ways downstream and needs manual attention.
                log.error("Could not disable stranded Keycloak user {} - manual cleanup required",
                        keycloakId, cleanupFailure);
            }
            throw e;
        }
    }

    /**
     * Triggers Keycloak's own password-reset email. Always reports success so the
     * endpoint cannot be used to probe which emails exist.
     */
    public void requestPasswordReset(String email) {
        keycloak.findByEmail(email.trim().toLowerCase()).ifPresentOrElse(
                user -> keycloak.sendPasswordReset((String) user.get("id")),
                () -> log.info("Password reset requested for an unknown email"));
    }

    /** Ends every session for the caller - used by "sign out of all devices". */
    public void logoutEverywhere(String keycloakId) {
        keycloak.logoutUser(keycloakId);
    }

    private static String randomPlaceholderPassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * @param created false when the email was already taken; callers must still
     *                return a generic response so the difference is not observable.
     */
    public record RegistrationResult(String keycloakId, String userId, boolean created) {

        public Map<String, Object> toClientResponse() {
            return Map.of("message",
                    "If the address is not already registered, a verification email has been sent.");
        }
    }
}
