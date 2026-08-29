package com.example.anusha.job_trail.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record ApplicationCreateRequest(
        @NotBlank @Size(max = 255) String company,
        @NotBlank @Size(max = 255) String role,
        @Size(max = 255) String location,
        @PositiveOrZero Integer salaryMin,
        @PositiveOrZero Integer salaryMax,
        @URL @Size(max = 2048) String link,
        @Size(max = 100) String source,
        @Size(max = 5000) String notes,
        LocalDate deadline,
        // The actual posting text — what /score is run against. Free-form,
        // no size cap match the ml-service scrape budget: pasted straight
        // from a posting, this can legitimately run to a few thousand words.
        String jobDescriptionText
) {

    @AssertTrue(message = "salaryMin must not be greater than salaryMax")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
