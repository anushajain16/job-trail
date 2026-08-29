package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.application.dto.ApplicationCreateRequest;
import com.example.anusha.job_trail.application.dto.ApplicationResponse;
import com.example.anusha.job_trail.application.dto.ApplicationUpdateRequest;
import com.example.anusha.job_trail.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Kept in the same package as {@link Application} rather than a `mapper`
 * subpackage: MapStruct's generated implementation instantiates the entity
 * via its protected no-args constructor, which is only visible package-local.
 */
@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    // Only used for matchedSkills/missingSkills's stored-JSON-string ->
    // List<String> conversion below — narrow enough a purpose that a
    // dedicated static instance (rather than the app's Spring-managed
    // ObjectMapper bean, which an interface's default method can't have
    // injected) is the simpler choice.
    JsonMapper SKILLS_JSON_MAPPER = JsonMapper.builder().build();

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentStage", ignore = true)
    @Mapping(target = "matchScore", ignore = true)
    @Mapping(target = "matchedSkills", ignore = true)
    @Mapping(target = "missingSkills", ignore = true)
    @Mapping(target = "scoredResumeProfileId", ignore = true)
    @Mapping(target = "scoredJdHash", ignore = true)
    @Mapping(target = "scoredAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Application toEntity(ApplicationCreateRequest request, User user);

    @Mapping(target = "resumeVersionId", source = "resumeVersion.id")
    @Mapping(target = "coverLetterVersionId", source = "coverLetterVersion.id")
    @Mapping(target = "matchedSkills", source = "matchedSkills", qualifiedByName = "parseSkillsJson")
    @Mapping(target = "missingSkills", source = "missingSkills", qualifiedByName = "parseSkillsJson")
    ApplicationResponse toResponse(Application application);

    @Named("parseSkillsJson")
    default List<String> parseSkillsJson(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return SKILLS_JSON_MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JacksonException e) {
            // Only reachable if this column was written by something other
            // than MatchScoringService's own JSON-array serialization.
            throw new IllegalStateException("Stored skills JSON is unreadable", e);
        }
    }

    // PATCH semantics: a null field in the request means "leave as is", not
    // "clear this field" — NullValuePropertyMappingStrategy.IGNORE is what
    // makes that the generated behavior instead of MapStruct's default of
    // copying nulls straight through. resumeVersion/coverLetterVersion are
    // resolved (and ownership-checked) by ApplicationService, not here —
    // the request only carries ids, not entities.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "currentStage", ignore = true)
    @Mapping(target = "resumeVersion", ignore = true)
    @Mapping(target = "coverLetterVersion", ignore = true)
    @Mapping(target = "matchScore", ignore = true)
    @Mapping(target = "matchedSkills", ignore = true)
    @Mapping(target = "missingSkills", ignore = true)
    @Mapping(target = "scoredResumeProfileId", ignore = true)
    @Mapping(target = "scoredJdHash", ignore = true)
    @Mapping(target = "scoredAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ApplicationUpdateRequest request, @MappingTarget Application application);
}
