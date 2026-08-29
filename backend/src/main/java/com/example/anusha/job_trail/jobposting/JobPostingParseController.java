package com.example.anusha.job_trail.jobposting;

import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import com.example.anusha.job_trail.jobposting.dto.ParseUrlRequest;
import com.example.anusha.job_trail.jobposting.dto.ParseUrlResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The "submit a URL, autofill the form" entry point behind
 * {@link JobPostingParseService}. {@code currentUser} isn't used for
 * anything today — parsing doesn't touch any per-user data — but the
 * endpoint stays behind the same auth as the rest of {@code /api/**}
 * ({@code SecurityConfig} permits no path here) rather than becoming an
 * open proxy onto the ml-service.
 */
@RestController
@RequestMapping("/api/job-postings")
public class JobPostingParseController {

    private final JobPostingParseService parseService;

    public JobPostingParseController(JobPostingParseService parseService) {
        this.parseService = parseService;
    }

    @PostMapping("/parse")
    public ParseUrlResponse parse(@CurrentUser AuthenticatedUser currentUser,
                                   @Valid @RequestBody ParseUrlRequest request) {
        return parseService.parseUrl(request.url());
    }
}
