package com.example.anusha.job_trail.matching.exception;

/**
 * Raised by {@link com.example.anusha.job_trail.matching.MlServiceMatchClient}
 * once the retry budget is exhausted (connection failure, timeout, or a 5xx
 * from ml-service) or a response was unusable. Unlike
 * {@code jobposting.exception.MlServiceUnavailableException}, this one is
 * never swallowed into a graceful fallback — there's no meaningful
 * "manual entry" equivalent for a match score, so
 * {@code GlobalExceptionHandler} maps it straight to a 502.
 */
public class MlServiceUnavailableException extends RuntimeException {

    public MlServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public MlServiceUnavailableException(String message) {
        super(message);
    }
}
