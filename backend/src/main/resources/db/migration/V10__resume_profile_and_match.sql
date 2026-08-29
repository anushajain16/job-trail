-- One structured profile per user, parsed from their resume by the
-- ml-service (POST /profile). Upserted in place on re-parse — this is
-- "the user's current profile", not a version history, hence the unique
-- user_id rather than a user_id index.
CREATE TABLE resume_profile (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    -- Which resume version this profile was parsed from. ON DELETE
    -- RESTRICT, not SET NULL/CASCADE: a profile with no traceable source
    -- document is meaningless (there'd be nothing to re-parse from and no
    -- way to explain a stored match score), so deleting that document
    -- must go through re-parsing (or deleting) the profile first.
    source_document_id  UUID NOT NULL REFERENCES documents (id) ON DELETE RESTRICT,
    -- The ml-service's ResumeProfile response body, verbatim, as JSON —
    -- skills/years_experience/roles/seniority/summary. Stored as opaque
    -- text rather than modeled as columns: nothing in this app queries
    -- inside it, only reads and re-serializes it whole (into /score calls
    -- and the frontend response).
    profile_json        TEXT NOT NULL,
    confidence           DOUBLE PRECISION NOT NULL,
    parsed_at            TIMESTAMPTZ NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Match results, cached on the application row itself — same "read-optimized
-- cache of the latest computed thing" pattern Application.currentStage uses
-- for status_history. All nullable: most existing applications, and any
-- application that's never been scored, simply have no match yet.
ALTER TABLE applications
    -- The JD text /score is actually run against. Not derived from
    -- anything else on this row (company/role/notes) — a real posting's
    -- full text, pasted or fetched via the parse-URL flow.
    ADD COLUMN job_description_text   TEXT,
    ADD COLUMN match_score            DOUBLE PRECISION,
    -- JSON string arrays (["python","fastapi"]), same "store what nothing
    -- queries inside" reasoning as resume_profile.profile_json.
    ADD COLUMN matched_skills         TEXT,
    ADD COLUMN missing_skills         TEXT,
    -- The two halves of the cache key MatchScoringService checks before
    -- calling ml-service again: which profile, and a hash of which JD
    -- text, produced the stored result. ON DELETE SET NULL: if the
    -- profile a score was computed from is gone (re-parsed to a new row),
    -- the cache key can never match again, so the stored score is now
    -- unverifiable provenance, not a broken foreign key to fix.
    ADD COLUMN scored_resume_profile_id UUID REFERENCES resume_profile (id) ON DELETE SET NULL,
    ADD COLUMN scored_jd_hash          VARCHAR(64),
    ADD COLUMN scored_at               TIMESTAMPTZ;
