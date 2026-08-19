ALTER TABLE applications
    ADD COLUMN current_stage VARCHAR(20) NOT NULL DEFAULT 'SAVED';

CREATE TABLE status_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    stage          VARCHAR(20) NOT NULL,
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The history endpoint and analytics both read one application's timeline
-- in chronological order — this is the hot path for both.
CREATE INDEX idx_status_history_application_id_changed_at
    ON status_history (application_id, changed_at);

-- Backfill: every application that already exists gets the SAVED row its
-- currentStage column implies, so the "history is the source of truth"
-- invariant holds for pre-existing rows too, not just new ones.
INSERT INTO status_history (application_id, stage, changed_at)
SELECT id, 'SAVED', created_at FROM applications;
