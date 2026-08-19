package com.example.anusha.job_trail.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ApplicationCreateRequest(
        @NotBlank @Size(max = 255) String company,
        @NotBlank @Size(max = 255) String role,
        @Size(max = 255) String location,
        @PositiveOrZero Integer salaryMin,
        @PositiveOrZero Integer salaryMax,
        @URL @Size(max = 2048) String link,
        @Size(max = 100) String source,
        @Size(max = 5000) String notes
) {

    @AssertTrue(message = "salaryMin must not be greater than salaryMax")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
