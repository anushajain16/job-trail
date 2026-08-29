package com.example.anusha.job_trail.user;

import com.example.anusha.job_trail.common.AbstractIntegrationTest;
import com.example.anusha.job_trail.common.config.FlywayConfig;
import com.example.anusha.job_trail.common.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @DataJpaTest is a narrow slice — it doesn't pick up plain @Configuration
 * beans the way the full app context does, so FlywayConfig and
 * JpaAuditingConfig are imported explicitly. Runs against a real Postgres started by Testcontainers
 * (see AbstractIntegrationTest), not an embedded DB.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FlywayConfig.class, JpaAuditingConfig.class})
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsByEmail() {
        User user = new User("ada@jobtrail.dev", "hashed-password");

        userRepository.saveAndFlush(user);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();

        Optional<User> found = userRepository.findByEmail("ada@jobtrail.dev");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
        assertThat(found.get().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void findByEmailIsEmptyForUnknownAddress() {
        assertThat(userRepository.findByEmail("nobody@jobtrail.dev")).isEmpty();
    }

    @Test
    void enforcesUniqueEmail() {
        userRepository.saveAndFlush(new User("dup@jobtrail.dev", "hash-a"));

        User duplicate = new User("dup@jobtrail.dev", "hash-b");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
