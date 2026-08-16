package com.example.anusha.job_trail.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests — no Spring context. Each test builds its own JwtService
 * so ttl/secret can be tuned per case (e.g. a negative ttl to produce an
 * already-expired token deterministically, instead of sleeping).
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long!!";

    private JwtService serviceWithTtl(Duration accessTokenTtl) {
        return new JwtService(new JwtProperties(SECRET, accessTokenTtl, Duration.ofDays(30)));
    }

    @Test
    void issuedTokenParsesBackToTheSameClaims() {
        JwtService jwtService = serviceWithTtl(Duration.ofMinutes(15));
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueAccessToken(userId, "ada@jobtrail.dev");
        Optional<AuthenticatedUser> parsed = jwtService.parse(token);

        assertThat(parsed).contains(new AuthenticatedUser(userId, "ada@jobtrail.dev"));
    }

    @Test
    void expiredTokenDoesNotParse() {
        // ttl already in the past at the moment of issuance — no waiting required.
        JwtService jwtService = serviceWithTtl(Duration.ofSeconds(-1));

        String token = jwtService.issueAccessToken(UUID.randomUUID(), "ada@jobtrail.dev");

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void tokenSignedWithADifferentSecretDoesNotParse() {
        JwtService issuer = serviceWithTtl(Duration.ofMinutes(15));
        JwtService verifier = new JwtService(new JwtProperties(
                "a-completely-different-secret-that-is-also-32b+", Duration.ofMinutes(15), Duration.ofDays(30)));

        String token = issuer.issueAccessToken(UUID.randomUUID(), "ada@jobtrail.dev");

        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    void tamperedTokenDoesNotParse() {
        JwtService jwtService = serviceWithTtl(Duration.ofMinutes(15));
        String token = jwtService.issueAccessToken(UUID.randomUUID(), "ada@jobtrail.dev");

        // Flip a character in the middle of the payload, not the last one:
        // base64url's final character can carry unused padding bits, so
        // some tail substitutions decode to identical bytes and wouldn't
        // actually tamper anything.
        int middle = token.length() / 2;
        char flipped = token.charAt(middle) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, middle) + flipped + token.substring(middle + 1);

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void garbageInputDoesNotParse() {
        JwtService jwtService = serviceWithTtl(Duration.ofMinutes(15));

        assertThat(jwtService.parse("not-a-jwt-at-all")).isEmpty();
    }
}
