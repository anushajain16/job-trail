package com.example.anusha.job_trail.document;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.common.config.FlywayConfig;
import com.example.anusha.job_trail.common.config.JpaAuditingConfig;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest is a narrow slice — it doesn't pick up plain @Configuration
 * beans the way the full app context does, so FlywayConfig and
 * JpaAuditingConfig are imported explicitly, same as ApplicationRepositoryTest.
 * Runs against the real Postgres behind the "test" profile, not an embedded DB.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FlywayConfig.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User newUser() {
        return userRepository.saveAndFlush(new User("user-" + UUID.randomUUID() + "@jobtrail.dev", "hash"));
    }

    private Document newDocument(User user, DocumentType type, String label) {
        return documentRepository.saveAndFlush(new Document(user, type, label,
                "key-" + UUID.randomUUID(), "resume.pdf", "application/pdf", 1024L));
    }

    @Test
    void savesAndSetsUploadedAt() {
        User user = newUser();
        Document document = newDocument(user, DocumentType.RESUME, "Backend-focused v1");

        assertThat(document.getId()).isNotNull();
        assertThat(document.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdAndUserId_isEmptyForAnotherUsersDocument() {
        User owner = newUser();
        User someoneElse = newUser();
        Document document = newDocument(owner, DocumentType.RESUME, "v1");

        Optional<Document> asOwner = documentRepository.findByIdAndUserId(document.getId(), owner.getId());
        Optional<Document> asStranger = documentRepository.findByIdAndUserId(document.getId(), someoneElse.getId());

        assertThat(asOwner).isPresent();
        assertThat(asStranger).isEmpty();
    }

    @Test
    void findByUserIdAndType_onlyReturnsMatchingTypeForThatUser() {
        User owner = newUser();
        User someoneElse = newUser();
        newDocument(owner, DocumentType.RESUME, "Resume v1");
        newDocument(owner, DocumentType.COVER_LETTER, "Cover letter v1");
        newDocument(someoneElse, DocumentType.RESUME, "Someone else's resume");

        List<Document> resumes = documentRepository.findByUserIdAndTypeOrderByCreatedAtDesc(owner.getId(), DocumentType.RESUME);

        assertThat(resumes).hasSize(1);
        assertThat(resumes.get(0).getLabel()).isEqualTo("Resume v1");
    }

    @Test
    void applicationCanLinkToAResumeVersionItOwns() {
        User user = newUser();
        Document resume = newDocument(user, DocumentType.RESUME, "Backend-focused v1");
        Document coverLetter = newDocument(user, DocumentType.COVER_LETTER, "Standard cover letter");

        Application application = new Application(user, "Anthropic", "Backend Engineer");
        application.setResumeVersion(resume);
        application.setCoverLetterVersion(coverLetter);
        applicationRepository.saveAndFlush(application);

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getResumeVersion().getId()).isEqualTo(resume.getId());
        assertThat(reloaded.getCoverLetterVersion().getId()).isEqualTo(coverLetter.getId());
    }

    @Test
    void deletingADocument_setsLinkedApplicationVersionToNull() {
        User user = newUser();
        Document resume = newDocument(user, DocumentType.RESUME, "Backend-focused v1");
        Application application = new Application(user, "Anthropic", "Backend Engineer");
        application.setResumeVersion(resume);
        applicationRepository.saveAndFlush(application);
        UUID resumeId = resume.getId();
        UUID applicationId = application.getId();

        // Clear first: with `application` still managed in this persistence
        // context and referencing `resume`, Hibernate's flush ordering trips
        // over the about-to-be-deleted association before the DELETE even
        // reaches Postgres. Deleting by id against a clean context sidesteps
        // that entirely and leaves the FK's ON DELETE SET NULL to do its job.
        entityManager.clear();
        documentRepository.deleteById(resumeId);
        documentRepository.flush();
        entityManager.clear();

        Application reloaded = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(reloaded.getResumeVersion()).isNull();
    }
}
