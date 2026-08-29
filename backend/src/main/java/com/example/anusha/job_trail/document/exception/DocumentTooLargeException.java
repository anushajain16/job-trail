package com.example.anusha.job_trail.document.exception;

/** Thrown when an upload exceeds {@code app.documents.max-size-bytes}. */
public class DocumentTooLargeException extends RuntimeException {

    public DocumentTooLargeException(long sizeBytes, long maxSizeBytes) {
        super("Document is " + sizeBytes + " bytes, which exceeds the " + maxSizeBytes + "-byte limit.");
    }
}
