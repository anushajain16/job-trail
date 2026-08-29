package com.example.anusha.job_trail.common;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for every test that needs a real Postgres and a real MinIO —
 * full {@code @SpringBootTest}s and {@code @DataJpaTest} slices alike.
 * Starts one {@link PostgreSQLContainer} and one {@link MinIOContainer} and
 * points {@code spring.datasource.*} / {@code app.documents.storage.*} at
 * them via {@code @DynamicPropertySource}, so the suite never depends on
 * the docker-compose db/MinIO being up: `mvn test` works the same on a
 * laptop and in CI, with nothing but a Docker daemon as a prerequisite.
 * MinIO is started here (not just in the document-flow test) because any
 * full-context test instantiates {@code MinioDocumentStorage}, which
 * connects to a real MinIO endpoint at startup.
 *
 * <p>Both containers are started once in a static initializer and never
 * stopped explicitly — this is Testcontainers' documented "singleton
 * container" pattern. Because the fields live on this class (not each
 * subclass), every test class that extends it shares the same running
 * containers and the same started-once cost; Testcontainers' Ryuk sidecar
 * reaps them when the JVM exits.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("jobtrail_test")
                    .withUsername("jobtrail")
                    .withPassword("jobtrail");

    protected static final MinIOContainer MINIO = new MinIOContainer("minio/minio:latest");

    static {
        POSTGRES.start();
        MINIO.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("app.documents.storage.endpoint", MINIO::getS3URL);
        registry.add("app.documents.storage.access-key", MINIO::getUserName);
        registry.add("app.documents.storage.secret-key", MINIO::getPassword);
        registry.add("app.documents.storage.bucket", () -> "job-trail-documents-test");
    }
}
