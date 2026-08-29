package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.common.persistence.BaseEntity;
import com.example.anusha.job_trail.document.Document;
import com.example.anusha.job_trail.status.Stage;
import com.example.anusha.job_trail.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;

/**
 * A single tracked job application: who it's for (company, role), where it
 * came from (source, link), and free-form notes. Always owned by exactly one
 * {@link User} — every repository query in this feature filters by that
 * owner, which is what keeps one user's applications invisible to another.
 *
 * <p>Status pipeline (Saved/Applied/Screen/...) is a separate, append-only
 * event log feature: {@code currentStage} below is a read-optimized cache
 * of "the stage of the latest status_history row", kept in sync only by
 * {@code StatusHistoryService.recordTransition}. Never assign it directly —
 * the history table, not this column, is the source of truth.
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

    // Optional: a posting's stated deadline, or a self-set follow-up date.
    // The only reader today is the auto-ghost job (see the scheduler
    // package) — it never acts on an application with no deadline set.
    private LocalDate deadline;

    // Which named resume/cover-letter version was actually sent for this
    // application, if any. Pinned by id, not "the user's latest resume" —
    // that's what lets resume-performance analytics attribute a response
    // (or its absence) to the exact version that went out, even after the
    // user has since uploaded newer ones. Set via
    // ApplicationService.update, which also enforces that the referenced
    // document belongs to this user and is the right DocumentType.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_version_id")
    private Document resumeVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_version_id")
    private Document coverLetterVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 20)
    private Stage currentStage = Stage.SAVED;

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
