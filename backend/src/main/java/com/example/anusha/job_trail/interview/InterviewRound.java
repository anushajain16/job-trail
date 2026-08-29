package com.example.anusha.job_trail.interview;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.common.persistence.BaseEntity;
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
 * One logged interview round for an {@link Application} — Screen,
 * Technical R1, Technical R2, Final, and so on. {@code roundType} is
 * free text rather than an enum: real pipelines name rounds in ways that
 * don't collapse into a fixed set ("Technical R1" vs "Technical R2" is
 * the same type of round, twice).
 *
 * <p>Unlike {@link com.example.anusha.job_trail.status.StatusHistory},
 * every field here is editable in place — {@code reflection} in
 * particular is meant to be written well after the round happens, and
 * refined again before the next one. There is no ownership column of its
 * own; every query is scoped through {@code application.user}.
 */
@Entity
@Table(name = "interview_round")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewRound extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "round_type", nullable = false, length = 100)
    private String roundType;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "interviewer_name", length = 255)
    private String interviewerName;

    // Free-form — one question per line is the expected shape, but nothing
    // here parses or validates that structure.
    @Column(name = "questions_asked", columnDefinition = "TEXT")
    private String questionsAsked;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // The part worth re-reading before the next round — kept as its own
    // column (not folded into notes) so the frontend can give it separate,
    // more prominent treatment.
    @Column(columnDefinition = "TEXT")
    private String reflection;

    // Set by CalendarSyncService after the first successful "Add to
    // Calendar" call, null until then. Its presence is exactly what tells
    // that service to update this event on Google's side rather than
    // create a duplicate on a second sync.
    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    // Not on BaseEntity: this entity updates in place (edited notes and
    // reflections are the whole point), unlike the append-only event logs
    // that don't declare one.
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InterviewRound(Application application, String roundType) {
        this.application = application;
        this.roundType = roundType;
    }
}
