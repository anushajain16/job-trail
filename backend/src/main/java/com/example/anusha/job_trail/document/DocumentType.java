package com.example.anusha.job_trail.document;

/**
 * What a {@link Document} row represents. Both types share the same upload,
 * storage, and ownership rules — only the semantics of "which slot on an
 * {@link com.example.anusha.job_trail.application.Application} it can fill"
 * differ, enforced where a version id is resolved, not here.
 */
public enum DocumentType {
    RESUME,
    COVER_LETTER
}
