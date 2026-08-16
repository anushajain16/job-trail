package com.example.anusha.job_trail.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on {@code @CreatedDate}/{@code @LastModifiedDate} support so
 * {@link com.example.anusha.job_trail.common.persistence.BaseEntity} (and
 * anything that adds its own audited fields) gets them populated
 * automatically via {@code AuditingEntityListener}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
