package com.example.anusha.job_trail.matching;

import com.example.anusha.job_trail.matching.exception.ResumeTextExtractionException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Bytes in, plain text out — the only thing standing between a stored
 * resume (PDF or DOCX, per {@code DocumentService}'s upload whitelist) and
 * ml-service's {@code POST /profile}, which takes text. Backed by Apache
 * Tika's format-detecting facade rather than a PDF-specific and a
 * DOCX-specific parser called by content type: one call site regardless of
 * which of the two allowed types the bytes actually are.
 */
@Component
public class ResumeTextExtractor {

    private final Tika tika = new Tika();

    public String extract(InputStream content) {
        try {
            return tika.parseToString(content);
        } catch (IOException | org.apache.tika.exception.TikaException e) {
            throw new ResumeTextExtractionException("Failed to extract text from resume", e);
        }
    }
}
