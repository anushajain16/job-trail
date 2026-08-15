package com.example.anusha.job_trail.common.exception;

/**
 * Thrown when a lookup by id (or other unique key) finds nothing, and the
 * caller should see a 404 rather than a null or an unchecked failure.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
