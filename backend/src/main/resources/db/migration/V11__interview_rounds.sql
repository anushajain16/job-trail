-- One editable row per logged interview round (Screen, Technical R1,
-- Final, ...). Unlike status_history this is a normal CRUD table, not an
-- append-only log — notes and the reflection get refined after the fact.
CREATE TABLE interview_round (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id   UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    round_type       VARCHAR(100) NOT NULL,
    scheduled_at     TIMESTAMPTZ,
    interviewer_name VARCHAR(255),
    questions_asked  TEXT,
    notes            TEXT,
    reflection       TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The rounds tab reads "every round for this application" as its one hot
-- path, ordered by scheduled_at at read time.
CREATE INDEX idx_interview_round_application_id ON interview_round (application_id);
