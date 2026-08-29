package com.example.anusha.job_trail.document.storage;

import com.example.anusha.job_trail.document.DocumentProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

/**
 * The real {@link DocumentStorage}: a MinIO/S3 bucket, addressed with the
 * MinIO Java SDK (S3-API-compatible, so this same client works unmodified
 * against real AWS S3 too — only {@code app.documents.storage.endpoint}
 * needs to change). Unit tests mock {@link DocumentStorage} instead of
 * exercising this class; the MinIO Testcontainer integration test is what
 * actually proves this implementation against a live server.
 */
@Component
public class MinioDocumentStorage implements DocumentStorage, InitializingBean {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioDocumentStorage(DocumentProperties properties) {
        this.minioClient = MinioClient.builder()
                .endpoint(properties.storage().endpoint())
                .credentials(properties.storage().accessKey(), properties.storage().secretKey())
                .build();
        this.bucket = properties.storage().bucket();
    }

    // Runs once at startup rather than per-call: bucket creation is the only
    // "does this exist yet" check this class needs, and doing it once keeps
    // every store/delete call from paying for a redundant existence check.
    @Override
    public void afterPropertiesSet() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    // -1 part size with a known object size tells the SDK to
                    // upload in one shot rather than multipart — every
                    // document here is well under any part-size threshold.
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to store document at key " + key, e);
        }
    }

    @Override
    public String presignedDownloadUrl(String key, String filename, Duration ttl) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) ttl.toSeconds())
                    // Makes the browser save the file under its original
                    // name instead of the opaque storage key.
                    .extraQueryParams(Map.of("response-content-disposition",
                            "attachment; filename=\"" + filename + "\""))
                    .build());
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to presign a download URL for key " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to delete document at key " + key, e);
        }
    }
}
