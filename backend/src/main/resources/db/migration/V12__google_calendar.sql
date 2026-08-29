-- One row per user who has connected Google Calendar. Only the refresh
-- token is stored (encrypted — see TokenCipher) since it's the only
-- long-lived credential; access tokens are minted on demand and never
-- persisted.
CREATE TABLE google_connection (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    encrypted_refresh_token TEXT NOT NULL,
    granted_scopes          VARCHAR(500) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Which Google Calendar event a round was last synced to, if any — null
-- until the first successful calendar-sync call. Present so a second sync
-- updates that event in place instead of creating a duplicate.
ALTER TABLE interview_round
    ADD COLUMN google_event_id VARCHAR(255);
