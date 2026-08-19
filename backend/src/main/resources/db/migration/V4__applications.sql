CREATE TABLE applications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company    VARCHAR(255) NOT NULL,
    role       VARCHAR(255) NOT NULL,
    location   VARCHAR(255),
    salary_min INTEGER,
    salary_max INTEGER,
    link       VARCHAR(2048),
    source     VARCHAR(100),
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Every application query in the app is scoped to "mine" — list, get, update,
-- delete all filter by user_id, so this index is on the hot path for all of it.
CREATE INDEX idx_applications_user_id ON applications (user_id);
