package com.example.anusha.job_trail.auth;

import com.example.anusha.job_trail.auth.exception.InvalidRefreshTokenException;
import com.example.anusha.job_trail.auth.security.JwtProperties;
import com.example.anusha.job_trail.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Owns the refresh-token table: issuing, validating, rotating, revoking.
 * The raw token handed to a client is a random 256-bit value, never
 * persisted — only its SHA-256 hash is, the same principle as password
 * storage, so a DB read alone can't be replayed as a token.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public String issue(User user) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), expiresAt));
        return rawToken;
    }

    /**
     * Validates a presented refresh token and revokes it — refresh tokens
     * are single-use, so both rotation (refresh) and logout consume the
     * token the same way; the caller decides whether to issue a
     * replacement. A token that's already revoked or expired being
     * presented again is treated as a signal of compromise: every other
     * active token for that user is revoked too, not just this one.
     */
    @Transactional
    public User verifyAndRevoke(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        if (!token.isActive()) {
            revokeAllActiveFor(token.getUser());
            throw new InvalidRefreshTokenException("Refresh token has already been used or has expired");
        }

        token.revoke();
        refreshTokenRepository.save(token);
        return token.getUser();
    }

    private void revokeAllActiveFor(User user) {
        List<RefreshToken> active = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId());
        active.forEach(RefreshToken::revoke);
        refreshTokenRepository.saveAll(active);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-guaranteed algorithm; this can't actually happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
