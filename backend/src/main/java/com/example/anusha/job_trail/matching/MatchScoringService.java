package com.example.anusha.job_trail.matching;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.matching.dto.MatchScoreResponse;
import com.example.anusha.job_trail.matching.dto.MlResumeProfile;
import com.example.anusha.job_trail.matching.dto.MlScoreResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Owns scoring an application's job description against the caller's
 * resume profile, and the cache that makes repeat calls cheap: a stored
 * result is reused as-is — no ml-service call — as long as both the
 * profile and the JD text it was computed from are still exactly what's
 * on the row now. Either one changing (a re-parsed resume, an edited JD)
 * invalidates it.
 */
@Service
public class MatchScoringService {

    private static final JsonMapper SKILLS_JSON_MAPPER = JsonMapper.builder().build();

    private final ApplicationRepository applicationRepository;
    private final ResumeProfileService resumeProfileService;
    private final MlServiceMatchClient mlServiceMatchClient;

    public MatchScoringService(ApplicationRepository applicationRepository, ResumeProfileService resumeProfileService,
                                MlServiceMatchClient mlServiceMatchClient) {
        this.applicationRepository = applicationRepository;
        this.resumeProfileService = resumeProfileService;
        this.mlServiceMatchClient = mlServiceMatchClient;
    }

    @Transactional
    public MatchScoreResponse score(UUID applicationId, UUID userId) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        String jobDescriptionText = application.getJobDescriptionText();
        if (jobDescriptionText == null || jobDescriptionText.isBlank()) {
            throw new IllegalArgumentException(
                    "Application " + applicationId + " has no job description text to score against");
        }

        ResumeProfile resumeProfile = resumeProfileService.findOwned(userId);
        String jdHash = sha256Hex(jobDescriptionText);

        boolean cacheHit = application.getScoredAt() != null
                && Objects.equals(application.getScoredResumeProfileId(), resumeProfile.getId())
                && Objects.equals(application.getScoredJdHash(), jdHash);

        if (cacheHit) {
            return new MatchScoreResponse(application.getMatchScore(), parseSkills(application.getMatchedSkills()),
                    parseSkills(application.getMissingSkills()), application.getScoredAt(), true);
        }

        MlResumeProfile profile = resumeProfileService.readProfileJson(resumeProfile.getProfileJson());
        MlScoreResponse mlResponse = mlServiceMatchClient.score(profile, jobDescriptionText);

        Instant scoredAt = Instant.now();
        application.setMatchScore(mlResponse.matchPct());
        application.setMatchedSkills(writeSkills(mlResponse.matchedSkills()));
        application.setMissingSkills(writeSkills(mlResponse.missingSkills()));
        application.setScoredResumeProfileId(resumeProfile.getId());
        application.setScoredJdHash(jdHash);
        application.setScoredAt(scoredAt);

        return new MatchScoreResponse(mlResponse.matchPct(), mlResponse.matchedSkills(), mlResponse.missingSkills(),
                scoredAt, false);
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-guaranteed algorithm (every conforming JVM
            // ships it) — this can't actually happen.
            throw new IllegalStateException(e);
        }
    }

    private static List<String> parseSkills(String json) {
        if (json == null) {
            return List.of();
        }
        return SKILLS_JSON_MAPPER.readValue(json, new TypeReference<List<String>>() {
        });
    }

    private static String writeSkills(List<String> skills) {
        return SKILLS_JSON_MAPPER.writeValueAsString(skills);
    }
}
