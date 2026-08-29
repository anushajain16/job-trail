package com.example.anusha.job_trail.matching;

import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.document.Document;
import com.example.anusha.job_trail.document.DocumentRepository;
import com.example.anusha.job_trail.document.DocumentType;
import com.example.anusha.job_trail.document.storage.DocumentStorage;
import com.example.anusha.job_trail.matching.dto.MlProfileResponse;
import com.example.anusha.job_trail.matching.dto.MlResumeProfile;
import com.example.anusha.job_trail.matching.dto.ResumeProfileResponse;
import com.example.anusha.job_trail.matching.exception.ResumeTextExtractionException;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Owns the resume_profile table: turning the caller's most recently
 * uploaded resume into a structured profile (text extraction here, LLM
 * extraction in ml-service) and persisting the result. {@link MatchScoringService}
 * is the only other reader of this table — it never re-derives a profile
 * itself, only reads whatever this service last stored.
 */
@Service
public class ResumeProfileService {

    private final ResumeProfileRepository resumeProfileRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final ResumeTextExtractor textExtractor;
    private final MlServiceMatchClient mlServiceMatchClient;
    private final ObjectMapper objectMapper;

    public ResumeProfileService(ResumeProfileRepository resumeProfileRepository, DocumentRepository documentRepository,
                                 UserRepository userRepository, DocumentStorage documentStorage,
                                 ResumeTextExtractor textExtractor, MlServiceMatchClient mlServiceMatchClient,
                                 ObjectMapper objectMapper) {
        this.resumeProfileRepository = resumeProfileRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentStorage = documentStorage;
        this.textExtractor = textExtractor;
        this.mlServiceMatchClient = mlServiceMatchClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Parses (or re-parses) the caller's profile from their most recently
     * uploaded resume document. Always does the full round trip — text
     * extraction, an ml-service call, a write — there's no "unchanged"
     * short-circuit here the way {@link MatchScoringService#score} has:
     * parsing is an explicit, occasional user action, not something run
     * speculatively on every page load, so there's no hot path to cache
     * against.
     */
    @Transactional
    public ResumeProfileResponse parse(UUID userId) {
        Document resumeDocument = documentRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, DocumentType.RESUME)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No resume uploaded yet"));

        String resumeText;
        try (InputStream content = documentStorage.open(resumeDocument.getStorageKey())) {
            resumeText = textExtractor.extract(content);
        } catch (IOException e) {
            throw new ResumeTextExtractionException("Failed to read stored resume", e);
        }

        MlProfileResponse mlResponse = mlServiceMatchClient.parseProfile(resumeText);
        String profileJson = objectMapper.writeValueAsString(mlResponse.profile());
        Instant parsedAt = Instant.now();

        ResumeProfile profile = resumeProfileRepository.findByUserId(userId)
                .map(existing -> {
                    existing.reparse(resumeDocument, profileJson, mlResponse.confidence(), parsedAt);
                    return existing;
                })
                .orElseGet(() -> {
                    User userRef = userRepository.getReferenceById(userId);
                    ResumeProfile created = new ResumeProfile(userRef, resumeDocument, profileJson,
                            mlResponse.confidence(), parsedAt);
                    resumeProfileRepository.save(created);
                    return created;
                });

        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ResumeProfileResponse get(UUID userId) {
        return toResponse(findOwned(userId));
    }

    @Transactional(readOnly = true)
    ResumeProfile findOwned(UUID userId) {
        return resumeProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No resume profile yet — parse a resume first"));
    }

    private ResumeProfileResponse toResponse(ResumeProfile profile) {
        return new ResumeProfileResponse(profile.getId(), profile.getSourceDocument().getId(),
                readProfileJson(profile.getProfileJson()), profile.getConfidence(), profile.getParsedAt());
    }

    MlResumeProfile readProfileJson(String profileJson) {
        try {
            return objectMapper.readValue(profileJson, MlResumeProfile.class);
        } catch (JacksonException e) {
            // Only reachable if a row was written by something other than
            // this service, which nothing else in this app does.
            throw new IllegalStateException("Stored resume profile JSON is unreadable", e);
        }
    }
}
