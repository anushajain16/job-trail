package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.common.persistence.BaseEntity;
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

import java.time.Instant;
import java.time.LocalDate;

/**
 * One row per (application, calendar day) the reminder sweep has attempted
 * to notify about — the row this feature's duplicate-send protection is
 * built on. {@link ReminderSender} inserts a {@code PENDING} row *before*
 * attempting the send; the unique constraint on
 * (application_id, reminder_date) means a second attempt for the same
 * application on the same day (a job retry, an app restart mid-sweep, more
 * than one instance scheduled) fails to claim a row at all and backs off
 * without ever calling the mailer — see {@link ReminderSender#sendReminder}.
 */
@Entity
@Table(name = "reminder_log")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    public ReminderLog(Application application, LocalDate reminderDate) {
        this.application = application;
        this.reminderDate = reminderDate;
        this.status = ReminderStatus.PENDING;
    }
}
