-- Named resume/cover-letter versions. Bytes live in object storage; this
-- row is metadata plus the key to find them again (see DocumentStorage).
-- Immutable once uploaded — there is no update path, only upload/delete.
CREATE TABLE documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type              VARCHAR(20) NOT NULL,
    label             VARCHAR(255) NOT NULL,
    storage_key       VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT NOT NULL,
    uploaded_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_documents_storage_key UNIQUE (storage_key)
);

-- Both "my resume versions" and "my resume versions, filtered by type"
-- (the upload picker, resume-performance) are the hot read paths here.
CREATE INDEX idx_documents_user_id_type ON documents (user_id, type);

-- Which version was actually sent for this application, if any. Nullable:
-- most existing applications, and plenty of new ones, will never set these.
-- ON DELETE SET NULL rather than CASCADE/RESTRICT: deleting a document a
-- user no longer wants shouldn't be blocked by, or take down, applications
-- that once referenced it — the link just goes away.
ALTER TABLE applications
    ADD COLUMN resume_version_id UUID REFERENCES documents (id) ON DELETE SET NULL,
    ADD COLUMN cover_letter_version_id UUID REFERENCES documents (id) ON DELETE SET NULL;

-- The resume-performance query groups applications by resume_version_id;
-- both partial indexes keep that (and the symmetric cover-letter lookup)
-- off a full table scan without wasting space on the common NULL case.
CREATE INDEX idx_applications_resume_version_id
    ON applications (resume_version_id) WHERE resume_version_id IS NOT NULL;
CREATE INDEX idx_applications_cover_letter_version_id
    ON applications (cover_letter_version_id) WHERE cover_letter_version_id IS NOT NULL;
