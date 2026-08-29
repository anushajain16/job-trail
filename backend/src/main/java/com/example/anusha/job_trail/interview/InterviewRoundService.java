package com.example.anusha.job_trail.interview;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.interview.dto.InterviewRoundCreateRequest;
import com.example.anusha.job_trail.interview.dto.InterviewRoundResponse;
import com.example.anusha.job_trail.interview.dto.InterviewRoundUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns the interview_round table. Every method threads the caller's user
 * id through the query itself — same "a row that isn't ours doesn't exist"
 * auth boundary as {@code ApplicationService} — so a mismatched id/user
 * pair is a 404, not a 403, whether the round is reached through its
 * owning application (list/create) or directly by its own id
 * (update/delete).
 */
@Service
public class InterviewRoundService {

    private final InterviewRoundRepository interviewRoundRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRoundMapper interviewRoundMapper;

    public InterviewRoundService(InterviewRoundRepository interviewRoundRepository,
                                  ApplicationRepository applicationRepository,
                                  InterviewRoundMapper interviewRoundMapper) {
        this.interviewRoundRepository = interviewRoundRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRoundMapper = interviewRoundMapper;
    }

    @Transactional(readOnly = true)
    public List<InterviewRoundResponse> list(UUID applicationId, UUID userId) {
        requireOwnedApplication(applicationId, userId);
        return interviewRoundRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId).stream()
                .map(interviewRoundMapper::toResponse)
                .toList();
    }

    @Transactional
    public InterviewRoundResponse create(UUID applicationId, UUID userId, InterviewRoundCreateRequest request) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        InterviewRound interviewRound = interviewRoundMapper.toEntity(request, application);
        interviewRoundRepository.save(interviewRound);
        return interviewRoundMapper.toResponse(interviewRound);
    }

    @Transactional
    public InterviewRoundResponse update(UUID id, UUID userId, InterviewRoundUpdateRequest request) {
        InterviewRound interviewRound = findOwned(id, userId);
        interviewRoundMapper.updateEntityFromRequest(request, interviewRound);
        return interviewRoundMapper.toResponse(interviewRound);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        interviewRoundRepository.delete(findOwned(id, userId));
    }

    private InterviewRound findOwned(UUID id, UUID userId) {
        return interviewRoundRepository.findByIdAndApplicationUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + id));
    }

    private void requireOwnedApplication(UUID applicationId, UUID userId) {
        if (!applicationRepository.findByIdAndUserId(applicationId, userId).isPresent()) {
            throw new ResourceNotFoundException("Application not found: " + applicationId);
        }
    }
}
