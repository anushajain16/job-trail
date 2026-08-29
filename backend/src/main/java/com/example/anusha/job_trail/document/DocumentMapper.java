package com.example.anusha.job_trail.document;

import com.example.anusha.job_trail.document.dto.DocumentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "uploadedAt", source = "createdAt")
    DocumentResponse toResponse(Document document);
}
