package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.application.dto.ApplicationCreateRequest;
import com.example.anusha.job_trail.application.dto.ApplicationResponse;
import com.example.anusha.job_trail.application.dto.ApplicationUpdateRequest;
import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.document.Document;
import com.example.anusha.job_trail.document.DocumentRepository;
import com.example.anusha.job_trail.document.DocumentType;
import com.example.anusha.job_trail.status.Stage;
import com.example.anusha.job_trail.status.StatusHistoryService;
import com.example.anusha.job_trail.status.dto.StatusHistoryResponse;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns the applications table. Every method takes the caller's user id and
 * threads it into the query itself (never "load then check the owner in
 * Java") — that's the actual auth boundary: a row that isn't ours doesn't
 * exist as far as this service is concerned, so a mismatched id/user pair
 * surfaces as 404, not 403.
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;
    private final StatusHistoryService statusHistoryService;
    private final DocumentRepository documentRepository;

    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository,
                               ApplicationMapper applicationMapper, StatusHistoryService statusHistoryService,
                               DocumentRepository documentRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.applicationMapper = applicationMapper;
        this.statusHistoryService = statusHistoryService;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> list(UUID userId, Pageable pageable) {
        return applicationRepository.findByUserId(userId, pageable).map(applicationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID id, UUID userId) {
        return applicationMapper.toResponse(findOwned(id, userId));
    }

    @Transactional
    public ApplicationResponse create(UUID userId, ApplicationCreateRequest request) {
        // A lazy reference, not a select-by-id: the caller's id already came
        // out of a validated access token, so there's no need to round-trip
        // the user row just to attach its id as a foreign key.
        User userRef = userRepository.getReferenceById(userId);
        Application application = applicationMapper.toEntity(request, userRef);
        applicationRepository.save(application);
        // The application's own "currentStage = SAVED" default is just a
        // column value until there's a matching history row — write it now
        // so the timeline always starts at creation, not at the first PATCH.
        statusHistoryService.recordInitial(application);
        return applicationMapper.toResponse(application);
    }

    @Transactional
    public ApplicationResponse update(UUID id, UUID userId, ApplicationUpdateRequest request) {
        Application application = findOwned(id, userId);
        applicationMapper.updateEntityFromRequest(request, application);
        if (request.resumeVersionId() != null) {
            application.setResumeVersion(resolveDocument(request.resumeVersionId(), userId, DocumentType.RESUME));
        }
        if (request.coverLetterVersionId() != null) {
            application.setCoverLetterVersion(resolveDocument(request.coverLetterVersionId(), userId, DocumentType.COVER_LETTER));
        }
        return applicationMapper.toResponse(application);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        applicationRepository.delete(findOwned(id, userId));
    }

    @Transactional
    public ApplicationResponse changeStage(UUID id, UUID userId, Stage newStage) {
        Application application = findOwned(id, userId);
        statusHistoryService.recordTransition(application, newStage);
        return applicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getHistory(UUID id, UUID userId) {
        Application application = findOwned(id, userId);
        return statusHistoryService.getHistory(application.getId());
    }

    private Application findOwned(UUID id, UUID userId) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    // Same ownership-scoped lookup as findOwned, plus a type check: a
    // resume version id can't be attached as a cover letter, or vice versa.
    private Document resolveDocument(UUID documentId, UUID userId, DocumentType expectedType) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (document.getType() != expectedType) {
            throw new IllegalArgumentException("Document " + documentId + " is not a " + expectedType + " version");
        }
        return document;
    }
}
