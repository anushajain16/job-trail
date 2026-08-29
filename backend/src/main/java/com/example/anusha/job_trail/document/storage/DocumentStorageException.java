package com.example.anusha.job_trail.document.storage;

/**
 * Wraps any failure talking to the underlying object store (network error,
 * bad credentials, ...) into one unchecked type so callers don't need to
 * know the storage backend's own checked-exception surface. Falls through
 * {@code GlobalExceptionHandler}'s catch-all to a 500 — there's no client
 * input that causes this, so there's nothing more specific to tell them.
 */
public class DocumentStorageException extends RuntimeException {

    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
