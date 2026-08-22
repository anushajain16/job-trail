package com.example.anusha.job_trail.user;

import com.example.anusha.job_trail.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The account a set of applications belongs to. Created during
 * registration (auth feature, not built yet) — nothing here is exposed as
 * a public API in this slice.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    // Nullable: a user who only ever signed in via Google/GitHub has no
    // local password. Set the first time they sign up with one, or (not
    // built yet) if they later add a password to an OAuth-only account.
    @Column(name = "password_hash")
    private String passwordHash;

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
