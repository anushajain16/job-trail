package com.example.anusha.job_trail.jobposting.exception;

/**
 * Raised by {@link com.example.anusha.job_trail.jobposting.MlServiceParseClient}
 * once the retry budget is exhausted (connection failure, timeout, or a 5xx
 * from ml-service) or the response was unusable. Never lets this escape to
 * {@code GlobalExceptionHandler} as a 502 — {@code JobPostingParseService}
 * catches it and returns a normal 200 with {@code available: false}, since
 * "the ml-service is down" is the documented, expected failure path, not an
 * application error.
 */
public class MlServiceUnavailableException extends RuntimeException {

    public MlServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public MlServiceUnavailableException(String message) {
        super(message);
    }
}
