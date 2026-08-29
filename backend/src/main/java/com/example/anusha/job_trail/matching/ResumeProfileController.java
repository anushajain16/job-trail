package com.example.anusha.job_trail.matching;

import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import com.example.anusha.job_trail.matching.dto.ResumeProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resume-profile")
public class ResumeProfileController {

    private final ResumeProfileService resumeProfileService;

    public ResumeProfileController(ResumeProfileService resumeProfileService) {
        this.resumeProfileService = resumeProfileService;
    }

    // Parses (or re-parses) the caller's profile from their most recently
    // uploaded resume — see ResumeProfileService.parse. No request body:
    // there's exactly one input, "whichever resume is currently the
    // caller's latest", and it's looked up server-side.
    @PostMapping("/parse")
    public ResumeProfileResponse parse(@CurrentUser AuthenticatedUser currentUser) {
        return resumeProfileService.parse(currentUser.id());
    }

    @GetMapping
    public ResumeProfileResponse get(@CurrentUser AuthenticatedUser currentUser) {
        return resumeProfileService.get(currentUser.id());
    }
}
