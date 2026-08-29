package com.example.anusha.job_trail.document.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * The only way {@link com.example.anusha.job_trail.document.DocumentService}
 * touches object storage — everything above this interface talks in terms of
 * an opaque {@code key}, never a bucket, an SDK client, or a URL scheme. That
 * split is what lets {@link MinioDocumentStorage} be swapped for a different
 * backend (or, in a unit test, a mock) without any of the DB-facing document
 * code changing.
 */
public interface DocumentStorage {

    /**
     * Uploads {@code content} under {@code key}, overwriting nothing that
     * already exists there (keys are generated per-upload, never reused).
     * Consumes the stream but does not close it — the caller owns its
     * lifecycle.
     */
    void store(String key, InputStream content, long size, String contentType);

    /**
     * A time-limited URL the client can download the object from directly,
     * without the request round-tripping through this app. {@code filename}
     * becomes the browser's suggested save-as name.
     */
    String presignedDownloadUrl(String key, String filename, Duration ttl);

    /**
     * Streams the object's bytes back into this app — unlike every other
     * method here, which either writes or hands the client a URL to fetch
     * from directly. The one caller that needs this is
     * {@code ResumeProfileService}: resume text extraction (Tika) has to
     * run server-side, so it needs the actual bytes, not a redirect.
     * Caller-closed, same contract as any {@code InputStream}.
     */
    InputStream open(String key);

    /** Deletes the object at {@code key}. A no-op if it doesn't exist. */
    void delete(String key);
}
