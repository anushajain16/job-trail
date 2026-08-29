package com.example.anusha.job_trail.jobposting;

import com.example.anusha.job_trail.jobposting.dto.MlParseResponse;
import com.example.anusha.job_trail.jobposting.dto.ParseUrlResponse;
import com.example.anusha.job_trail.jobposting.exception.MlServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The graceful-fallback boundary this whole feature is built around: a
 * caller of {@link #parseUrl} never sees an exception for "ml-service is
 * down/slow/erroring" — only {@link ParseUrlResponse#available()} going
 * false. Manual entry is always the fallback, never a 5xx.
 */
@Service
public class JobPostingParseService {

    private static final Logger log = LoggerFactory.getLogger(JobPostingParseService.class);

    private final MlServiceParseClient client;

    public JobPostingParseService(MlServiceParseClient client) {
        this.client = client;
    }

    public ParseUrlResponse parseUrl(String url) {
        try {
            MlParseResponse response = client.parseUrl(url);
            return ParseUrlResponse.of(response.parsed(), response.confidence());
        } catch (MlServiceUnavailableException e) {
            log.warn("ml-service unavailable, falling back to manual entry: {}", e.getMessage());
            return ParseUrlResponse.unavailable(
                    "Couldn't auto-fill from that URL right now — enter the details manually.");
        }
    }
}
