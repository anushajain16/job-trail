package com.example.anusha.job_trail.document;

import com.example.anusha.job_trail.common.persistence.BaseEntity;
import com.example.anusha.job_trail.user.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single named, immutable upload — a resume or cover-letter version — with
 * its bytes held in object storage under {@link #storageKey} and everything
 * else the app needs (owner, label, original filename, content type, size)
 * kept here. There is no update path: a new version is a new row, which is
 * exactly what lets {@code Application.resumeVersionId} pin an application to
 * the exact bytes that were sent, forever, even if the user later uploads a
 * "v2" under a different row.
 *
 * <p>Reuses {@link BaseEntity}'s {@code @CreatedDate} machinery for the
 * upload timestamp, renamed to {@code uploaded_at} to match the domain
 * language, the same pattern {@code StatusHistory} uses for {@code changed_at}.
 */
@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "uploaded_at", nullable = false, updatable = false))
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type;

    @Column(nullable = false)
    private String label;

    // Object storage key, e.g. "{userId}/{uuid}-{originalFilename}" — the
    // only thing DocumentStorage needs to find these bytes again. Never
    // exposed to clients; a download hands out a presigned URL instead.
    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long size;

    public Document(User user, DocumentType type, String label, String storageKey,
                     String originalFilename, String contentType, long size) {
        this.user = user;
        this.type = type;
        this.label = label;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
    }
}
