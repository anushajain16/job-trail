package com.example.anusha.job_trail.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and validates access tokens only — refresh tokens are opaque,
 * DB-backed values handled by {@code RefreshTokenService}, not JWTs. Access
 * tokens are never persisted or looked up: {@link #parse} either yields a
 * principal straight from the signed claims or nothing, with no DB round
 * trip, which is what makes this "stateless" auth rather than session auth.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    public String issueAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies signature and expiry and, if both hold, extracts the
     * principal. Any failure — bad signature, malformed token, expired,
     * anything — collapses to {@code Optional.empty()}: the caller (the
     * auth filter) doesn't need to distinguish why a token didn't work,
     * only that it didn't.
     */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            return Optional.of(new AuthenticatedUser(userId, email));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
