package com.example.anusha.job_trail.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Reusable base for every entity in the app: an app-generated UUID primary
 * key (no round trip to the DB for an identity value, and no leaking
 * sequential ids) plus a Spring Data-managed {@code createdAt}. Extend this
 * rather than re-declaring id/createdAt on each entity.
 *
 * <p>Entities that also need to track updates should add their own
 * {@code @LastModifiedDate} field — not every entity wants one (e.g. the
 * append-only status event log never should), so it's not forced here.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
