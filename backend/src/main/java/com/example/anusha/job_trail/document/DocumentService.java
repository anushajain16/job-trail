package com.example.anusha.job_trail.document;

import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.document.dto.DocumentDownloadResponse;
import com.example.anusha.job_trail.document.dto.DocumentResponse;
import com.example.anusha.job_trail.document.exception.DocumentTooLargeException;
import com.example.anusha.job_trail.document.exception.UnsupportedDocumentTypeException;
import com.example.anusha.job_trail.document.storage.DocumentStorage;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the documents table and the uploaded bytes behind it. Every method
 * takes the caller's user id and threads it into the query itself — same
 * ownership pattern as {@code ApplicationService}: a document that isn't
 * ours doesn't exist as far as this service is concerned, so a mismatched
 * id/user pair surfaces as 404.
 *
 * <p>Upload validation (type, size) runs entirely here, before anything
 * touches {@link DocumentStorage} — a rejected upload never writes a byte or
 * a row.
 */
@Service
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final DocumentMapper documentMapper;
    private final DocumentProperties properties;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository,
                            DocumentStorage documentStorage, DocumentMapper documentMapper,
                            DocumentProperties properties) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentStorage = documentStorage;
        this.documentMapper = documentMapper;
        this.properties = properties;
    }

    @Transactional
    public DocumentResponse upload(UUID userId, DocumentType type, String label, MultipartFile file) {
        validate(file);

        String key = buildStorageKey(userId, file.getOriginalFilename());
        try (InputStream content = file.getInputStream()) {
            documentStorage.store(key, content, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }

        // A lazy reference, not a select-by-id — same reasoning as
        // ApplicationService.create: the caller's id already came out of a
        // validated access token.
        User userRef = userRepository.getReferenceById(userId);
        Document document = new Document(userRef, type, label, key,
                file.getOriginalFilename(), file.getContentType(), file.getSize());
        documentRepository.save(document);
        return documentMapper.toResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID userId, DocumentType type) {
        List<Document> documents = type == null
                ? documentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : documentRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
        return documents.stream().map(documentMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownloadResponse download(UUID id, UUID userId) {
        Document document = findOwned(id, userId);
        var ttl = properties.storage().presignedUrlTtl();
        String url = documentStorage.presignedDownloadUrl(document.getStorageKey(), document.getOriginalFilename(), ttl);
        return new DocumentDownloadResponse(url, document.getOriginalFilename(), document.getContentType(),
                Instant.now().plus(ttl));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        Document document = findOwned(id, userId);
        // Storage delete before the row delete: if storage fails, the row
        // (and the application links pointing at it) stay intact rather
        // than pointing at a key that no longer resolves anywhere.
        documentStorage.delete(document.getStorageKey());
        documentRepository.delete(document);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedDocumentTypeException(file.getContentType());
        }
        if (file.getSize() > properties.maxSizeBytes()) {
            throw new DocumentTooLargeException(file.getSize(), properties.maxSizeBytes());
        }
    }

    // One key per upload, namespaced by owner so two users can never collide
    // even if they upload identically named files.
    private static String buildStorageKey(UUID userId, String originalFilename) {
        return userId + "/" + UUID.randomUUID() + "-" + sanitize(originalFilename);
    }

    private static String sanitize(String filename) {
        return filename == null ? "document" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    Document findOwned(UUID id, UUID userId) {
        return documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }
}
