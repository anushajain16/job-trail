package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.application.dto.ApplicationCreateRequest;
import com.example.anusha.job_trail.application.dto.ApplicationResponse;
import com.example.anusha.job_trail.application.dto.ApplicationUpdateRequest;
import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository,
                               ApplicationMapper applicationMapper) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.applicationMapper = applicationMapper;
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
        return applicationMapper.toResponse(application);
    }

    @Transactional
    public ApplicationResponse update(UUID id, UUID userId, ApplicationUpdateRequest request) {
        Application application = findOwned(id, userId);
        applicationMapper.updateEntityFromRequest(request, application);
        return applicationMapper.toResponse(application);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        applicationRepository.delete(findOwned(id, userId));
    }

    private Application findOwned(UUID id, UUID userId) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }
}
