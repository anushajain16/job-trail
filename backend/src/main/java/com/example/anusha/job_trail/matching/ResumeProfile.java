package com.example.anusha.job_trail.matching;

import com.example.anusha.job_trail.common.persistence.BaseEntity;
import com.example.anusha.job_trail.document.Document;
import com.example.anusha.job_trail.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * The caller's current parsed resume — one row per user (see the unique
 * constraint on {@code user_id}), upserted in place by
 * {@link ResumeProfileService#parse} rather than versioned: this is "the
 * user's profile", not a history of every past parse.
 *
 * <p>{@code profileJson} holds ml-service's {@code ResumeProfile} response
 * body verbatim (skills, years of experience, roles, seniority, summary) —
 * nothing here queries inside it, only reads and re-serializes it whole
 * (into every {@code /score} call and this feature's own API response), so
 * there's no reason to model it as columns.
 */
@Entity
@Table(name = "resume_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Which resume version this was parsed from — traceability for the
    // stored profile, and what ResumeProfileService re-reads from on the
    // next parse. Never the source of the profile's *content* after that:
    // once parsed, this row is independent of the document until re-parsed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_document_id", nullable = false)
    private Document sourceDocument;

    @Column(name = "profile_json", nullable = false, columnDefinition = "TEXT")
    private String profileJson;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "parsed_at", nullable = false)
    private Instant parsedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ResumeProfile(User user, Document sourceDocument, String profileJson, double confidence, Instant parsedAt) {
        this.user = user;
        this.sourceDocument = sourceDocument;
        this.profileJson = profileJson;
        this.confidence = confidence;
        this.parsedAt = parsedAt;
    }

    /** The upsert path: re-parsing replaces this row's content in place —
     * see the class doc for why this isn't a new row instead. */
    public void reparse(Document sourceDocument, String profileJson, double confidence, Instant parsedAt) {
        this.sourceDocument = sourceDocument;
        this.profileJson = profileJson;
        this.confidence = confidence;
        this.parsedAt = parsedAt;
    }
}
