package com.example.anusha.job_trail.common;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for every test that needs a real Postgres — full
 * {@code @SpringBootTest}s and {@code @DataJpaTest} slices alike. Starts one
 * {@link PostgreSQLContainer} and points {@code spring.datasource.*} at it
 * via {@code @DynamicPropertySource}, so the suite never depends on the
 * docker-compose db being up: `mvn test` works the same on a laptop and in
 * CI, with nothing but a Docker daemon as a prerequisite.
 *
 * <p>The container is started once in a static initializer and never
 * stopped explicitly — this is Testcontainers' documented "singleton
 * container" pattern. Because the field lives on this class (not each
 * subclass), every test class that extends it shares the same running
 * container and the same started-once cost; Testcontainers' Ryuk sidecar
 * reaps it when the JVM exits.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("jobtrail_test")
                    .withUsername("jobtrail")
                    .withPassword("jobtrail");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
