package com.shopfast.userservice.service;

import com.shopfast.userservice.dto.UpdateProfileRequest;
import com.shopfast.userservice.enums.Role;
import com.shopfast.userservice.enums.UserStatus;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Resolves the profile row belonging to a signed-in Keycloak identity.
 *
 * <p>Keycloak is the source of truth for <em>who</em> someone is; this table holds
 * what the shop needs to know about them beyond that. The two can therefore be out
 * of step, and this class is where that is reconciled.
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the caller's profile, creating it if this is their first visit.
     *
     * <p>Auto-provisioning is not a convenience, it is a correctness requirement: users
     * are created in Keycloak (by self-registration, by an admin, by the seeding
     * script) and no code path guarantees a matching row here. Without this, a
     * perfectly valid account would get a 404 on its own profile page.
     *
     * <p>Resolution order matters:
     * <ol>
     *   <li>by {@code sub} - the durable link;</li>
     *   <li>by email - adopts a pre-Keycloak row and stamps the {@code sub} onto it,
     *       so the account keeps its existing orders instead of silently starting
     *       over behind a second, empty profile;</li>
     *   <li>create.</li>
     * </ol>
     */
    @Transactional
    public User getOrCreate(Jwt jwt) {
        String subject = jwt.getSubject();

        return userRepository.findByKeycloakId(subject)
                .map(user -> refreshFromToken(user, jwt))
                .orElseGet(() -> adoptOrCreate(jwt, subject));
    }

    @Transactional
    public User update(Jwt jwt, UpdateProfileRequest request) {
        User user = getOrCreate(jwt);

        // Null means "not supplied" and leaves the stored value alone, so a caller
        // sending only { phone } does not blank the user's name. Contrast this with
        // Keycloak's own PUT /users/{id}, which replaces the whole representation -
        // exactly the trap that blanked emails in the seeding script.
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(emptyToNull(request.phone()));
        }
        if (request.country() != null) {
            user.setCountry(emptyToNull(request.country()));
        }

        return userRepository.save(user);
    }

    private User adoptOrCreate(Jwt jwt, String subject) {
        String email = email(jwt);

        if (StringUtils.hasText(email)) {
            var existing = userRepository.findByEmailIgnoreCase(email);
            if (existing.isPresent()) {
                User user = existing.get();
                log.info("Linking existing profile {} to Keycloak subject {}", user.getId(), subject);
                user.setKeycloakId(subject);
                return userRepository.save(refreshFromToken(user, jwt));
            }
        }

        User user = new User();
        user.setKeycloakId(subject);
        user.setEmail(StringUtils.hasText(email) ? email : subject + "@unknown.local");
        user.setFirstName(claimOr(jwt, "given_name", "New"));
        user.setLastName(claimOr(jwt, "family_name", "User"));
        user.setRole(Role.ROLE_USER);
        user.setStatus(UserStatus.ACTIVE);

        try {
            log.info("Provisioning profile for Keycloak subject {}", subject);
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests (the profile page commonly fires more than one)
            // can both miss the lookup and both insert. The unique constraint makes
            // the loser fail rather than create a duplicate; re-reading is correct.
            log.debug("Concurrent provisioning for {}, re-reading", subject);
            return userRepository.findByKeycloakId(subject)
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Keeps the local copy in step with names the user may have changed in Keycloak.
     * Only fills fields we would otherwise be showing as stale or blank; it never
     * overwrites a value the user set here with an empty token claim.
     */
    private User refreshFromToken(User user, Jwt jwt) {
        boolean changed = false;

        String email = email(jwt);
        if (StringUtils.hasText(email) && !email.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }
        if (!StringUtils.hasText(user.getFirstName())) {
            user.setFirstName(claimOr(jwt, "given_name", "New"));
            changed = true;
        }
        if (!StringUtils.hasText(user.getLastName())) {
            user.setLastName(claimOr(jwt, "family_name", "User"));
            changed = true;
        }
        if (user.getRole() == null) {
            user.setRole(Role.ROLE_USER);
            changed = true;
        }
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
            changed = true;
        }

        return changed ? userRepository.save(user) : user;
    }

    private String email(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return StringUtils.hasText(email) ? email : jwt.getClaimAsString("preferred_username");
    }

    private String claimOr(Jwt jwt, String claim, String fallback) {
        String value = jwt.getClaimAsString(claim);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
