package com.shopfast.apigateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The routing decision here decides which key verifies a token, so getting it wrong
 * is an authentication bypass rather than a cosmetic bug. These tests pin the routing
 * itself; the underlying decoders are stubbed because their verification behaviour is
 * Spring's to test, not ours.
 */
class DualReactiveJwtDecoderTest {

    private static String rs256Token;
    private static String hs256Token;

    @BeforeAll
    static void createTokens() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("test").generate();
        JWSObject rs = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(),
                new Payload("{\"sub\":\"alice\"}"));
        rs.sign(new RSASSASigner(rsaKey));
        rs256Token = rs.serialize();

        JWSObject hs = new JWSObject(
                new JWSHeader(JWSAlgorithm.HS256),
                new Payload("{\"sub\":\"bob\"}"));
        hs.sign(new MACSigner("a-secret-that-is-at-least-32-bytes-long!!"));
        hs256Token = hs.serialize();
    }

    private static Jwt stubJwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    /** Records the token it was handed so the test can assert on routing. */
    private static ReactiveJwtDecoder recording(AtomicReference<String> sink, String subject) {
        return token -> {
            sink.set(token);
            return Mono.just(stubJwt(subject));
        };
    }

    @Test
    @DisplayName("routes an RS256 token to the Keycloak decoder")
    void routesRs256ToKeycloak() {
        AtomicReference<String> keycloakSaw = new AtomicReference<>();
        AtomicReference<String> legacySaw = new AtomicReference<>();

        var decoder = new DualReactiveJwtDecoder(
                recording(keycloakSaw, "keycloak"), recording(legacySaw, "legacy"));

        StepVerifier.create(decoder.decode(rs256Token))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo("keycloak"))
                .verifyComplete();

        assertThat(keycloakSaw.get()).isEqualTo(rs256Token);
        assertThat(legacySaw.get()).isNull();
    }

    @Test
    @DisplayName("routes an HS256 token to the legacy decoder")
    void routesHs256ToLegacy() {
        AtomicReference<String> keycloakSaw = new AtomicReference<>();
        AtomicReference<String> legacySaw = new AtomicReference<>();

        var decoder = new DualReactiveJwtDecoder(
                recording(keycloakSaw, "keycloak"), recording(legacySaw, "legacy"));

        StepVerifier.create(decoder.decode(hs256Token))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo("legacy"))
                .verifyComplete();

        assertThat(legacySaw.get()).isEqualTo(hs256Token);
        assertThat(keycloakSaw.get()).isNull();
    }

    @Test
    @DisplayName("rejects HS256 outright once the legacy decoder is switched off")
    void rejectsHs256WhenLegacyDisabled() {
        // This is the migration kill switch: unsetting JWT_SECRET must stop all
        // legacy tokens rather than quietly falling through to the Keycloak decoder,
        // which would then be asked to verify an HMAC token with an RSA key.
        var decoder = new DualReactiveJwtDecoder(
                recording(new AtomicReference<>(), "keycloak"), null);

        StepVerifier.create(decoder.decode(hs256Token))
                .expectError(BadJwtException.class)
                .verify();
    }

    @Test
    @DisplayName("rejects a malformed token instead of leaking a parse exception")
    void rejectsMalformedToken() {
        var decoder = new DualReactiveJwtDecoder(
                recording(new AtomicReference<>(), "keycloak"),
                recording(new AtomicReference<>(), "legacy"));

        StepVerifier.create(decoder.decode("not-a-jwt"))
                .expectError(BadJwtException.class)
                .verify();
    }

    @Test
    @DisplayName("rejects an unsigned (alg=none) token")
    void rejectsAlgNone() throws Exception {
        // alg=none is the oldest JWT attack there is: no signature to check, so any
        // decoder that treats it as "valid but unverified" hands out free admin tokens.
        String none = new com.nimbusds.jwt.PlainJWT(
                new com.nimbusds.jwt.JWTClaimsSet.Builder().subject("mallory").build()).serialize();

        var decoder = new DualReactiveJwtDecoder(
                recording(new AtomicReference<>(), "keycloak"),
                recording(new AtomicReference<>(), "legacy"));

        StepVerifier.create(decoder.decode(none))
                .expectError(BadJwtException.class)
                .verify();
    }
}
