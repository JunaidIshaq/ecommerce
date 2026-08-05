package com.shopfast.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Rejects tokens that were not minted for us.
 *
 * <p>Signature + issuer checks alone only prove "Keycloak issued this". In a realm
 * with several clients that is not enough: a token handed to a low-trust frontend
 * would otherwise be replayable against these APIs. Checking {@code aud} pins the
 * token to its intended recipient.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audiences = token.getAudience();
        if (audiences != null && audiences.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "Required audience '" + expectedAudience + "' is missing from the token",
                "https://tools.ietf.org/html/rfc6750#section-3.1"));
    }
}
