package com.example.anusha.job_trail.interview;

import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import com.example.anusha.job_trail.googlecalendar.CalendarSyncService;
import com.example.anusha.job_trail.interview.dto.InterviewRoundCreateRequest;
import com.example.anusha.job_trail.interview.dto.InterviewRoundResponse;
import com.example.anusha.job_trail.interview.dto.InterviewRoundUpdateRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

/**
 * List/create are nested under the owning application (there's no
 * "give me every round across every application" use case, and
 * {@code applicationId} is required to create one anyway); update/delete
 * are flat by the round's own id, same shape as
 * {@code DocumentController} — a round is addressed on its own once it
 * exists.
 */
@RestController
public class InterviewRoundController {

    private final InterviewRoundService interviewRoundService;
    private final InterviewRoundExportService interviewRoundExportService;
    private final CalendarSyncService calendarSyncService;

    public InterviewRoundController(InterviewRoundService interviewRoundService,
                                     InterviewRoundExportService interviewRoundExportService,
                                     CalendarSyncService calendarSyncService) {
        this.interviewRoundService = interviewRoundService;
        this.interviewRoundExportService = interviewRoundExportService;
        this.calendarSyncService = calendarSyncService;
    }

    // A literal path — no {id} sibling on this controller to conflict
    // with. Every round across every application the caller owns, as a
    // downloadable CSV.
    @GetMapping("/api/interviews/export")
    public void export(@CurrentUser AuthenticatedUser currentUser, HttpServletResponse response) {
        interviewRoundExportService.export(currentUser.id(), response);
    }

    @GetMapping("/api/applications/{applicationId}/interviews")
    public List<InterviewRoundResponse> list(@CurrentUser AuthenticatedUser currentUser,
                                              @PathVariable UUID applicationId) {
        return interviewRoundService.list(applicationId, currentUser.id());
    }

    @PostMapping("/api/applications/{applicationId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewRoundResponse create(@CurrentUser AuthenticatedUser currentUser,
                                          @PathVariable UUID applicationId,
                                          @Valid @RequestBody InterviewRoundCreateRequest request) {
        return interviewRoundService.create(applicationId, currentUser.id(), request);
    }

    @PatchMapping("/api/interviews/{id}")
    public InterviewRoundResponse update(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id,
                                          @Valid @RequestBody InterviewRoundUpdateRequest request) {
        return interviewRoundService.update(id, currentUser.id(), request);
    }

    @DeleteMapping("/api/interviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        interviewRoundService.delete(id, currentUser.id());
    }

    // "Add to Calendar" — creates the event the first time, updates the
    // same event (by its stored google_event_id) on every call after
    // that, so clicking it again never duplicates. See CalendarSyncService.
    @PostMapping("/api/interviews/{id}/calendar-sync")
    public InterviewRoundResponse calendarSync(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        return calendarSyncService.sync(id, currentUser.id());
    }
}
