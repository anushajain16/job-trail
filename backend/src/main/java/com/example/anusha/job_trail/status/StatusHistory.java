package com.example.anusha.job_trail.status;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.common.persistence.BaseEntity;
import jakarta.persistence.AttributeOverride;
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

/**
 * One row per stage transition, and never anything else — there is no
 * setter here and no update path anywhere in the codebase, because this
 * table is the append-only source of truth the subway map and analytics
 * are built from. {@link Application#getCurrentStage()} is just a cached
 * read of "the stage of this application's latest row"; if the two ever
 * disagree, this table is right.
 *
 * <p>Reuses {@link BaseEntity}'s {@code @CreatedDate} machinery for the
 * transition timestamp, renamed to {@code changed_at} to match the domain
 * language (there's nothing else on this row that could later "change").
 */
@Entity
@Table(name = "status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "changed_at", nullable = false, updatable = false))
public class StatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Stage stage;

    public StatusHistory(Application application, Stage stage) {
        this.application = application;
        this.stage = stage;
    }
}
