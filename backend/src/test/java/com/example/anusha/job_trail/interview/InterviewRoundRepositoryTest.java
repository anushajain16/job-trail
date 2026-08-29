package com.example.anusha.job_trail.interview;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.common.AbstractIntegrationTest;
import com.example.anusha.job_trail.common.config.FlywayConfig;
import com.example.anusha.job_trail.common.config.JpaAuditingConfig;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest is a narrow slice — FlywayConfig and JpaAuditingConfig are
 * imported explicitly the same way ApplicationRepositoryTest does. Runs
 * against a real Postgres started by Testcontainers (see
 * AbstractIntegrationTest).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FlywayConfig.class, JpaAuditingConfig.class})
class InterviewRoundRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private InterviewRoundRepository interviewRoundRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    private User newUser() {
        return userRepository.saveAndFlush(new User("user-" + UUID.randomUUID() + "@jobtrail.dev", "hash"));
    }

    private Application newApplication(User user) {
        return applicationRepository.saveAndFlush(new Application(user, "Anthropic", "Backend Engineer"));
    }

    @Test
    void savesAndSetsAuditFields() {
        Application application = newApplication(newUser());
        InterviewRound round = new InterviewRound(application, "Screen");

        interviewRoundRepository.saveAndFlush(round);

        assertThat(round.getId()).isNotNull();
        assertThat(round.getCreatedAt()).isNotNull();
        assertThat(round.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByApplicationIdOrderByScheduledAtAsc_ordersChronologically() {
        Application application = newApplication(newUser());
        InterviewRound later = new InterviewRound(application, "Final");
        later.setScheduledAt(Instant.parse("2026-09-10T00:00:00Z"));
        InterviewRound earlier = new InterviewRound(application, "Screen");
        earlier.setScheduledAt(Instant.parse("2026-09-01T00:00:00Z"));
        interviewRoundRepository.saveAndFlush(later);
        interviewRoundRepository.saveAndFlush(earlier);

        List<InterviewRound> rounds = interviewRoundRepository.findByApplicationIdOrderByScheduledAtAsc(application.getId());

        assertThat(rounds).extracting(InterviewRound::getRoundType).containsExactly("Screen", "Final");
    }

    @Test
    void findByIdAndApplicationUserId_isEmptyForAnotherUsersRound() {
        User owner = newUser();
        User someoneElse = newUser();
        InterviewRound round = interviewRoundRepository.saveAndFlush(new InterviewRound(newApplication(owner), "Screen"));

        Optional<InterviewRound> asOwner = interviewRoundRepository.findByIdAndApplicationUserId(round.getId(), owner.getId());
        Optional<InterviewRound> asStranger = interviewRoundRepository.findByIdAndApplicationUserId(round.getId(), someoneElse.getId());

        assertThat(asOwner).isPresent();
        assertThat(asStranger).isEmpty();
    }
}
