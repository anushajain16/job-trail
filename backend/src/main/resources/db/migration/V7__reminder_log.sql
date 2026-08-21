-- One row per (application, calendar day) the reminder sweep has ever
-- attempted to notify about. The unique constraint is what makes reminder
-- delivery idempotent: claiming a row happens before the email is sent, so
-- a re-run of the same day's sweep (job retry, app restart mid-run, more
-- than one instance scheduled) can never send the same day's reminder
-- twice — the second attempt just hits a constraint violation and backs off.
CREATE TABLE reminder_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    reminder_date  DATE NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at        TIMESTAMPTZ,
    CONSTRAINT uk_reminder_log_application_id_reminder_date UNIQUE (application_id, reminder_date)
);
