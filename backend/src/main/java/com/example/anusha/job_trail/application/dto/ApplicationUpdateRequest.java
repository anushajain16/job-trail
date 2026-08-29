package com.example.anusha.job_trail.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.UUID;

/**
 * PATCH body: every field is optional, and a field left out of the request
 * (null here) leaves the stored value untouched — there's no way to null out
 * a field this way, but that's out of scope for this slice. Fields that
 * *are* present still have to pass the same shape constraints as on create.
 *
 * <p>{@code resumeVersionId}/{@code coverLetterVersionId} point at a
 * document the caller owns; {@code ApplicationService} resolves and
 * ownership-checks them (a document id that isn't theirs, or isn't the
 * right {@code DocumentType}, is rejected — it can't be mapped here since
 * that check needs a repository lookup).
 */
public record ApplicationUpdateRequest(
        @Size(max = 255) String company,
        @Size(max = 255) String role,
        @Size(max = 255) String location,
        @PositiveOrZero Integer salaryMin,
        @PositiveOrZero Integer salaryMax,
        @URL @Size(max = 2048) String link,
        @Size(max = 100) String source,
        @Size(max = 5000) String notes,
        LocalDate deadline,
        UUID resumeVersionId,
        UUID coverLetterVersionId
) {

    @AssertTrue(message = "salaryMin must not be greater than salaryMax")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
