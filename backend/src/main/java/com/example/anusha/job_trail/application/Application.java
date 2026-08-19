package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.common.persistence.BaseEntity;
import com.example.anusha.job_trail.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * A single tracked job application: who it's for (company, role), where it
 * came from (source, link), and free-form notes. Always owned by exactly one
 * {@link User} — every repository query in this feature filters by that
 * owner, which is what keeps one user's applications invisible to another.
 *
 * <p>Status pipeline (Saved/Applied/Screen/...) is a separate, append-only
 * event log feature and deliberately isn't a column here.
 */
@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String role;

    private String location;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    private String link;

    private String source;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Not on BaseEntity: most entities never update in place (e.g. the
    // status event log), but an Application does — that's exactly what
    // PATCH /{id} is for — so this one tracks it.
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Application(User user, String company, String role) {
        this.user = user;
        this.company = company;
        this.role = role;
    }
}
