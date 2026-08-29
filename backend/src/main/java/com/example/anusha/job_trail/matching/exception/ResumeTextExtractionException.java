package com.example.anusha.job_trail.matching.exception;

/**
 * Wraps a failure reading text out of a stored resume's bytes (a corrupt
 * PDF, an unsupported-in-practice DOCX variant Tika can't parse, ...) into
 * one unchecked type — same role {@code DocumentStorageException} plays for
 * object-storage failures. Falls through {@code GlobalExceptionHandler}'s
 * catch-all to a 500: there's no user input that causes this (the upload
 * itself already passed {@code DocumentService}'s content-type check), so
 * there's nothing more specific to tell them.
 */
public class ResumeTextExtractionException extends RuntimeException {

    public ResumeTextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
