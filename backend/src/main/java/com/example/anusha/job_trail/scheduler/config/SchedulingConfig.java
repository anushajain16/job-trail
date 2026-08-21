package com.example.anusha.job_trail.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Turns on Spring's {@code @Scheduled} machinery for the whole app. Kept as
 * its own one-line config (rather than the annotation living on
 * {@code JobTrailApplication}) so the two background-job concerns —
 * scheduling and async execution — are each a single, obvious place to look.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    // A single injectable Clock, not LocalDate.now()/Instant.now() sprinkled
    // through the job services — that's what lets a test freeze "now" to a
    // fixed instant instead of racing the real system clock.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
