-- Optional target date the applicant is tracking for this application (a
-- posting's stated deadline, or a self-set follow-up date). Nullable: most
-- rows created before this feature, and plenty created after it, will never
-- have one. Nothing reads this column yet except the auto-ghost job, which
-- treats "no deadline" as "not eligible" rather than inventing one.
ALTER TABLE applications
    ADD COLUMN deadline DATE;

-- The auto-ghost job's candidate scan filters on both columns together
-- (deadline elapsed, then updated_at stale) every run, so a composite index
-- matching that predicate order keeps it off a full table scan as the table
-- grows.
CREATE INDEX idx_applications_deadline_updated_at
    ON applications (deadline, updated_at)
    WHERE deadline IS NOT NULL;
