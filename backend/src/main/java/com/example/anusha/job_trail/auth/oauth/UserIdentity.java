package com.example.anusha.job_trail.auth.oauth;

import com.example.anusha.job_trail.common.persistence.BaseEntity;
import com.example.anusha.job_trail.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Links a {@link User} to one external provider identity (e.g. a Google
 * account's {@code sub} claim). A user can have several — one per linked
 * provider, plus optionally a local password — which is why this is its
 * own table rather than columns on {@code User}.
 */
@Entity
@Table(name = "user_identities",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_identities_provider_subject",
                columnNames = {"provider", "provider_subject"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdentity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    // The provider's stable, immutable subject identifier for this account
    // (Google's `sub` claim; GitHub's numeric user id as a string) — never
    // the email, which a user can change on the provider's side.
    @Column(name = "provider_subject", nullable = false)
    private String providerSubject;

    public UserIdentity(User user, AuthProvider provider, String providerSubject) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
    }
}
