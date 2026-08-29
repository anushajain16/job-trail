package com.example.anusha.job_trail.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Every read in this feature is scoped to the caller's own rows — same
    // ownership pattern as ApplicationRepository: a document that isn't ours
    // doesn't exist as far as any of these queries are concerned.
    List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Document> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, DocumentType type);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);
}
