-- Baseline migration: proves Flyway is wired to Postgres and gives later
-- migrations (users, applications, status_events, ...) gen_random_uuid().
-- No business tables yet.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
