/**
 * Named resume/cover-letter versions: upload, list, download (via a
 * presigned storage URL), delete, and the link from an
 * {@link com.example.anusha.job_trail.application.Application} to the
 * specific version sent. Bytes live in object storage behind the
 * {@link com.example.anusha.job_trail.document.storage.DocumentStorage}
 * abstraction — this table only ever holds metadata and a storage key.
 */
package com.example.anusha.job_trail.document;
