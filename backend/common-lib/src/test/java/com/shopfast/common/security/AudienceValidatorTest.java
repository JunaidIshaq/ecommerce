package com.shopfast.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private static Jwt jwtWithAudience(List<String> audience) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("a-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (audience != null) {
            builder.audience(audience);
        }
        return builder.build();
    }

    @Test
    @DisplayName("accepts a token that lists the expected audience")
    void acceptsMatchingAudience() {
        var result = new AudienceValidator("shopfast-api")
                .validate(jwtWithAudience(List.of("shopfast-api", "account")));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("rejects a token minted for a different client")
    void rejectsForeignAudience() {
        // This is the case the validator exists for: a valid, correctly signed token
        // from the same realm that was never intended for these APIs.
        var result = new AudienceValidator("shopfast-api")
                .validate(jwtWithAudience(List.of("some-other-client")));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("rejects a token with no audience claim at all")
    void rejectsMissingAudience() {
        var result = new AudienceValidator("shopfast-api").validate(jwtWithAudience(null));

        assertThat(result.hasErrors()).isTrue();
    }
}
