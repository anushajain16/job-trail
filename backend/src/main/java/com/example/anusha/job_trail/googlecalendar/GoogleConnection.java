package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.common.persistence.BaseEntity;
import com.example.anusha.job_trail.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * A user's connected Google Calendar — one row per user (see the unique
 * constraint on {@code user_id}), same "upsert in place, not a history"
 * shape as {@code matching.ResumeProfile}. {@code encryptedRefreshToken}
 * is the one thing here that's actually secret (see {@link TokenCipher});
 * {@code grantedScopes} is stored mainly so a future feature that needs a
 * different scope can tell whether a re-connect is required without
 * calling Google to find out.
 */
@Entity
@Table(name = "google_connection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleConnection extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "granted_scopes", nullable = false, length = 500)
    private String grantedScopes;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GoogleConnection(User user, String encryptedRefreshToken, String grantedScopes) {
        this.user = user;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.grantedScopes = grantedScopes;
    }

    /** The upsert path: reconnecting (e.g. after revoking access on
     * Google's side, or granting an additional scope) replaces this row's
     * credential in place rather than creating a second one. */
    public void reconnect(String encryptedRefreshToken, String grantedScopes) {
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.grantedScopes = grantedScopes;
    }
}
