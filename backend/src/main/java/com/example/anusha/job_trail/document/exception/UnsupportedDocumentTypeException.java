package com.example.anusha.job_trail.document.exception;

/** Thrown when an upload's content type isn't one of the allowed PDF/DOCX types. */
public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException(String contentType) {
        super("Unsupported document type: " + contentType + ". Only PDF and DOCX are accepted.");
    }
}
