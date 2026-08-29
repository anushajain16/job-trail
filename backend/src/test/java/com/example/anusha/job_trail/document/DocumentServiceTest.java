package com.example.anusha.job_trail.document;

import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.document.dto.DocumentDownloadResponse;
import com.example.anusha.job_trail.document.dto.DocumentResponse;
import com.example.anusha.job_trail.document.exception.DocumentTooLargeException;
import com.example.anusha.job_trail.document.exception.UnsupportedDocumentTypeException;
import com.example.anusha.job_trail.document.storage.DocumentStorage;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises DocumentService's own logic (validation, ownership, key
 * building) with {@link DocumentStorage} mocked out — no MinIO involved.
 * The real storage implementation is proven separately, by the MinIO
 * Testcontainer integration test.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentStorage documentStorage;

    private DocumentService documentService;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        DocumentProperties properties = new DocumentProperties(1_000_000L,
                new DocumentProperties.Storage("http://localhost:9000", "key", "secret", "bucket", Duration.ofMinutes(10)));
        documentService = new DocumentService(documentRepository, userRepository, documentStorage,
                new DocumentMapperImpl(), properties);
    }

    private MockMultipartFile pdf(String content) {
        return new MockMultipartFile("file", "resume.pdf", "application/pdf", content.getBytes());
    }

    @Test
    void upload_storesBytesAndSavesMetadata() {
        User user = new User("user@jobtrail.dev", "hash");
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.upload(USER_ID, DocumentType.RESUME, "Backend-focused v1", pdf("%PDF-1.4 fake content"));

        assertThat(response.label()).isEqualTo("Backend-focused v1");
        assertThat(response.type()).isEqualTo(DocumentType.RESUME);
        assertThat(response.originalFilename()).isEqualTo("resume.pdf");
        verify(documentStorage).store(anyString(), any(), anyLong(), eq("application/pdf"));
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void upload_rejectsUnsupportedContentType_andNeverTouchesStorage() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> documentService.upload(USER_ID, DocumentType.RESUME, "v1", file))
                .isInstanceOf(UnsupportedDocumentTypeException.class);

        verify(documentStorage, never()).store(anyString(), any(), anyLong(), anyString());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void upload_rejectsOversizedFile_andNeverTouchesStorage() {
        DocumentProperties tinyLimit = new DocumentProperties(4L,
                new DocumentProperties.Storage("http://localhost:9000", "key", "secret", "bucket", Duration.ofMinutes(10)));
        documentService = new DocumentService(documentRepository, userRepository, documentStorage,
                new DocumentMapperImpl(), tinyLimit);

        assertThatThrownBy(() -> documentService.upload(USER_ID, DocumentType.RESUME, "v1", pdf("way more than four bytes")))
                .isInstanceOf(DocumentTooLargeException.class);

        verify(documentStorage, never()).store(anyString(), any(), anyLong(), anyString());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void download_returnsPresignedUrlForOwnedDocument() {
        User user = new User("user@jobtrail.dev", "hash");
        Document document = new Document(user, DocumentType.RESUME, "v1", "storage-key", "resume.pdf", "application/pdf", 42L);
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findByIdAndUserId(documentId, USER_ID)).thenReturn(Optional.of(document));
        when(documentStorage.presignedDownloadUrl(eq("storage-key"), eq("resume.pdf"), any(Duration.class)))
                .thenReturn("https://minio.local/presigned");

        DocumentDownloadResponse response = documentService.download(documentId, USER_ID);

        assertThat(response.downloadUrl()).isEqualTo("https://minio.local/presigned");
        assertThat(response.filename()).isEqualTo("resume.pdf");
    }

    @Test
    void download_throwsNotFound_whenDocumentBelongsToAnotherUser() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findByIdAndUserId(documentId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.download(documentId, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesFromStorageThenFromTheRepository() {
        User user = new User("user@jobtrail.dev", "hash");
        Document document = new Document(user, DocumentType.RESUME, "v1", "storage-key", "resume.pdf", "application/pdf", 42L);
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findByIdAndUserId(documentId, USER_ID)).thenReturn(Optional.of(document));

        documentService.delete(documentId, USER_ID);

        verify(documentStorage, times(1)).delete("storage-key");
        verify(documentRepository, times(1)).delete(document);
    }
}
