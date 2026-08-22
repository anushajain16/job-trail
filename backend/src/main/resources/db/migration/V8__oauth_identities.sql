-- Password is no longer guaranteed: a user who signs up via Google/GitHub
-- has no local password until (if ever) they set one.
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

-- One row per linked provider identity. A user can accumulate more than one
-- (password + Google, Google + GitHub, ...); the same provider subject is
-- looked up on every OAuth login, hence the unique constraint doubling as
-- the lookup key.
CREATE TABLE user_identities (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, provider_subject)
);

-- "Does this user already have a linked identity" is checked on account
-- lookups; keeps that off a full scan as the table grows.
CREATE INDEX idx_user_identities_user_id ON user_identities (user_id);
