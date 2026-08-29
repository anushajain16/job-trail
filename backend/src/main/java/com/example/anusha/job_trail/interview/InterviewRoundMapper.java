package com.example.anusha.job_trail.interview;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.interview.dto.InterviewRoundCreateRequest;
import com.example.anusha.job_trail.interview.dto.InterviewRoundResponse;
import com.example.anusha.job_trail.interview.dto.InterviewRoundUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Kept in the same package as {@link InterviewRound} rather than a
 * `mapper` subpackage — same reason as {@code ApplicationMapper}:
 * MapStruct's generated implementation needs the protected no-args
 * constructor, which is only visible package-local.
 */
@Mapper(componentModel = "spring")
public interface InterviewRoundMapper {

    // Both source params have a "notes" property (Application's own notes
    // field vs. the request's) — spelled out explicitly so MapStruct
    // doesn't have to guess which one this entity's notes should come from.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "notes", source = "request.notes")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InterviewRound toEntity(InterviewRoundCreateRequest request, Application application);

    @Mapping(target = "applicationId", source = "application.id")
    InterviewRoundResponse toResponse(InterviewRound interviewRound);

    // PATCH semantics: a null field in the request means "leave as is" —
    // same NullValuePropertyMappingStrategy.IGNORE as
    // ApplicationMapper#updateEntityFromRequest.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "application", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(InterviewRoundUpdateRequest request, @MappingTarget InterviewRound interviewRound);
}
