package com.example.anusha.job_trail.document;

import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import com.example.anusha.job_trail.document.dto.DocumentDownloadResponse;
import com.example.anusha.job_trail.document.dto.DocumentResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Validated
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@CurrentUser AuthenticatedUser currentUser,
                                    @RequestParam @NotNull DocumentType type,
                                    @RequestParam @NotBlank @Size(max = 255) String label,
                                    @RequestParam("file") MultipartFile file) {
        return documentService.upload(currentUser.id(), type, label, file);
    }

    @GetMapping
    public List<DocumentResponse> list(@CurrentUser AuthenticatedUser currentUser,
                                        @RequestParam(required = false) DocumentType type) {
        return documentService.list(currentUser.id(), type);
    }

    @GetMapping("/{id}")
    public DocumentDownloadResponse download(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        return documentService.download(id, currentUser.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID id) {
        documentService.delete(id, currentUser.id());
    }
}
