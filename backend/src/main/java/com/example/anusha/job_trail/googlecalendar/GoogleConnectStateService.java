package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.auth.security.JwtProperties;
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
 * Signs and verifies the {@code state} query param carried through the
 * connect flow: {@code POST /connect} is authenticated (a normal fetch with
 * a bearer token), but Google's redirect back to {@code GET /callback} is a
 * full-page browser navigation that can't carry one — this short-lived,
 * single-purpose JWT is what ties that callback back to the user who
 * started the flow, without a server-side session or an extra DB table.
 *
 * <p>Reuses {@code JwtProperties}' signing key rather than adding a
 * dedicated one — the {@code purpose} claim keeps a state token from being
 * confused with a real access token (or vice versa) even though both are
 * signed with the same key.
 */
@Service
public class GoogleConnectStateService {

    private static final String PURPOSE = "google-calendar-connect";
    private static final String CLAIM_PURPOSE = "purpose";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final SecretKey key;

    public GoogleConnectStateService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_PURPOSE, PURPOSE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(STATE_TTL)))
                .signWith(key)
                .compact();
    }

    /** Any failure — bad signature, expired, wrong purpose, malformed —
     * collapses to empty; the controller doesn't need to distinguish why. */
    public Optional<UUID> verify(String state) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(state).getPayload();
            if (!PURPOSE.equals(claims.get(CLAIM_PURPOSE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
