/**
 * "Submit a URL, autofill the form": the Spring side of the ml-service
 * integration. {@link com.example.anusha.job_trail.jobposting.JobPostingParseController}
 * takes a job posting URL, {@link com.example.anusha.job_trail.jobposting.MlServiceParseClient}
 * calls M1's {@code POST /parse} over the internal network (shared-secret
 * header, hard timeout, limited retries), and
 * {@link com.example.anusha.job_trail.jobposting.JobPostingParseService} is
 * the boundary that turns any failure there into a graceful "fall back to
 * manual entry" response rather than an error — see {@code ml-service/README.md}'s
 * "Graceful degradation, by design" section for the mirror of this story on
 * the ml-service side.
 */
package com.example.anusha.job_trail.jobposting;
