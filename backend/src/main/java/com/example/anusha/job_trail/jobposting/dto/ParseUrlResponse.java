package com.example.anusha.job_trail.jobposting.dto;

/**
 * Always a 200 — the ml-service being unreachable, slow, or erroring is a
 * normal outcome here, not a failure of this endpoint. {@code available}
 * is what the frontend branches on: {@code true} means autofill the form
 * from {@code parsed}; {@code false} means fall back to manual entry, with
 * {@code message} as an optional reason to show the user.
 */
public record ParseUrlResponse(
        boolean available,
        String message,
        ParsedJobPosting parsed,
        Double confidence
) {

    public static ParseUrlResponse of(ParsedJobPosting parsed, double confidence) {
        return new ParseUrlResponse(true, null, parsed, confidence);
    }

    public static ParseUrlResponse unavailable(String message) {
        return new ParseUrlResponse(false, message, null, null);
    }
}
