package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.common.config.FlywayConfig;
import com.example.anusha.job_trail.common.config.JpaAuditingConfig;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest is a narrow slice — it doesn't pick up plain @Configuration
 * beans the way the full app context does, so FlywayConfig and
 * JpaAuditingConfig are imported explicitly. Runs against the real Postgres
 * behind the "test" profile (see application.yml), not an embedded DB.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FlywayConfig.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    private User newUser() {
        return userRepository.saveAndFlush(new User("user-" + UUID.randomUUID() + "@jobtrail.dev", "hash"));
    }

    @Test
    void savesAndSetsAuditFields() {
        User user = newUser();
        Application application = new Application(user, "Anthropic", "Backend Engineer");

        applicationRepository.saveAndFlush(application);

        assertThat(application.getId()).isNotNull();
        assertThat(application.getCreatedAt()).isNotNull();
        assertThat(application.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByIdAndUserId_isEmptyForAnotherUsersApplication() {
        User owner = newUser();
        User someoneElse = newUser();
        Application application = applicationRepository.saveAndFlush(new Application(owner, "Anthropic", "SWE"));

        Optional<Application> asOwner = applicationRepository.findByIdAndUserId(application.getId(), owner.getId());
        Optional<Application> asStranger = applicationRepository.findByIdAndUserId(application.getId(), someoneElse.getId());

        assertThat(asOwner).isPresent();
        assertThat(asStranger).isEmpty();
    }

    @Test
    void findByUserId_onlyReturnsThatUsersApplications_andIsPaginated() {
        User owner = newUser();
        User someoneElse = newUser();
        applicationRepository.saveAndFlush(new Application(owner, "Company A", "Role A"));
        applicationRepository.saveAndFlush(new Application(owner, "Company B", "Role B"));
        applicationRepository.saveAndFlush(new Application(someoneElse, "Company C", "Role C"));

        Page<Application> firstPage = applicationRepository.findByUserId(owner.getId(), PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getContent().get(0).getUser().getId()).isEqualTo(owner.getId());
    }
}
