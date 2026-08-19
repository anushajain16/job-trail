package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.application.dto.ApplicationCreateRequest;
import com.example.anusha.job_trail.application.dto.ApplicationResponse;
import com.example.anusha.job_trail.application.dto.ApplicationUpdateRequest;
import com.example.anusha.job_trail.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Kept in the same package as {@link Application} rather than a `mapper`
 * subpackage: MapStruct's generated implementation instantiates the entity
 * via its protected no-args constructor, which is only visible package-local.
 */
@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Application toEntity(ApplicationCreateRequest request, User user);

    ApplicationResponse toResponse(Application application);

    // PATCH semantics: a null field in the request means "leave as is", not
    // "clear this field" — NullValuePropertyMappingStrategy.IGNORE is what
    // makes that the generated behavior instead of MapStruct's default of
    // copying nulls straight through.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ApplicationUpdateRequest request, @MappingTarget Application application);
}
