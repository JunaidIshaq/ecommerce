package com.shopfast.apigateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.text.ParseException;

/**
 * Accepts both token families during the Keycloak migration.
 *
 * <p>Routing is by the JOSE header {@code alg}, not by trial-and-error:
 * <ul>
 *   <li>RS256 -> Keycloak, verified against the realm's public JWKS.</li>
 *   <li>HS256 -> legacy auth-service, verified with the shared secret.</li>
 * </ul>
 *
 * <p>The algorithm is read from the header but never trusted as an instruction -
 * each branch hands the token to a decoder that is hard-wired to exactly one
 * algorithm, which is what prevents the classic "alg confusion" attack where a
 * caller downgrades an RS256 token to HS256 and signs it with the public key.
 *
 * <p>This class exists only for the migration window. Once no legacy tokens remain
 * in circulation (i.e. after the longest refresh-token lifetime has elapsed since
 * the frontends switched), delete it and use the Keycloak decoder directly.
 */
public class DualReactiveJwtDecoder implements ReactiveJwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(DualReactiveJwtDecoder.class);

    private final ReactiveJwtDecoder keycloakDecoder;
    private final ReactiveJwtDecoder legacyDecoder;

    public DualReactiveJwtDecoder(ReactiveJwtDecoder keycloakDecoder, ReactiveJwtDecoder legacyDecoder) {
        this.keycloakDecoder = keycloakDecoder;
        this.legacyDecoder = legacyDecoder;
    }

    @Override
    public Mono<Jwt> decode(String token) {
        JWSAlgorithm algorithm;
        try {
            algorithm = (JWSAlgorithm) JWTParser.parse(token).getHeader().getAlgorithm();
        } catch (ParseException | ClassCastException e) {
            return Mono.error(new BadJwtException("Malformed JWT", e));
        }

        if (JWSAlgorithm.RS256.equals(algorithm)) {
            return keycloakDecoder.decode(token);
        }

        if (JWSAlgorithm.HS256.equals(algorithm)) {
            if (legacyDecoder == null) {
                return Mono.error(new BadJwtException(
                        "Legacy HS256 tokens are no longer accepted; sign in again to get a Keycloak token"));
            }
            // Logged at WARN so the migration has an observable finish line: when this
            // stops appearing, the legacy path can be deleted.
            log.warn("Legacy HS256 token accepted - client has not migrated to Keycloak yet");
            return legacyDecoder.decode(token);
        }

        return Mono.error(new BadJwtException("Unsupported JWT algorithm: " + algorithm));
    }
}
