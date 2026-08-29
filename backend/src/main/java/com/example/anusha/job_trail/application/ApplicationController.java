package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.application.dto.ApplicationCreateRequest;
import com.example.anusha.job_trail.application.dto.ApplicationResponse;
import com.example.anusha.job_trail.application.dto.ApplicationUpdateRequest;
import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import com.example.anusha.job_trail.matching.MatchScoringService;
import com.example.anusha.job_trail.matching.dto.MatchScoreResponse;
import com.example.anusha.job_trail.status.dto.StageChangeRequest;
import com.example.anusha.job_trail.status.dto.StatusHistoryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final MatchScoringService matchScoringService;

    public ApplicationController(ApplicationService applicationService, MatchScoringService matchScoringService) {
        this.applicationService = applicationService;
        this.matchScoringService = matchScoringService;
    }

    @GetMapping
    public Page<ApplicationResponse> list(@CurrentUser AuthenticatedUser currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return applicationService.list(currentUser.id(), pageable);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        return applicationService.get(id, currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@CurrentUser AuthenticatedUser currentUser,
                                       @Valid @RequestBody ApplicationCreateRequest request) {
        return applicationService.create(currentUser.id(), request);
    }

    @PatchMapping("/{id}")
    public ApplicationResponse update(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id,
                                       @Valid @RequestBody ApplicationUpdateRequest request) {
        return applicationService.update(id, currentUser.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        applicationService.delete(id, currentUser.id());
    }

    @PatchMapping("/{id}/stage")
    public ApplicationResponse changeStage(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id,
                                            @Valid @RequestBody StageChangeRequest request) {
        return applicationService.changeStage(id, currentUser.id(), request.stage());
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryResponse> history(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        return applicationService.getHistory(id, currentUser.id());
    }

    // See matching.MatchScoringService — resume ↔ JD match %, cached until
    // either the resume profile or this application's job description text
    // changes. Requires job_description_text to already be set (400 if
    // not) and a parsed resume profile to exist (404 if not) — this
    // endpoint scores against what's already stored, it doesn't collect
    // either input itself.
    @PostMapping("/{id}/score")
    public MatchScoreResponse score(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        return matchScoringService.score(id, currentUser.id());
    }
}
