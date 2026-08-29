package com.example.anusha.job_trail.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context, the real (dockerized) Postgres
 * behind the "test" profile, and a real MinIO server started in a
 * Testcontainer — the one test in this feature that exercises
 * {@link com.example.anusha.job_trail.document.storage.MinioDocumentStorage}
 * itself rather than a mock. Covers the actual upload/download round trip
 * and the auth-boundary requirement: one user's documents must be
 * invisible (and undownloadable) to another, even by guessing a valid id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DocumentFlowIntegrationTest {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("app.documents.storage.endpoint", minio::getS3URL);
        registry.add("app.documents.storage.access-key", minio::getUserName);
        registry.add("app.documents.storage.secret-key", minio::getPassword);
        registry.add("app.documents.storage.bucket", () -> "job-trail-documents-test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@jobtrail.dev";
    }

    private String newUserAccessToken() throws Exception {
        String email = uniqueEmail();
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "correct-horse-battery"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uploadResume(String token, String label, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);
        MvcResult result = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("type", "RESUME")
                        .param("label", label)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    @Test
    void uploadAndDownload_roundTripsThroughRealMinio() throws Exception {
        String token = newUserAccessToken();
        String id = uploadResume(token, "Backend-focused v1", "%PDF-1.4 fake resume bytes".getBytes());

        mockMvc.perform(get("/api/documents").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].label").value("Backend-focused v1"));

        mockMvc.perform(get("/api/documents/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("resume.pdf"))
                .andExpect(jsonPath("$.downloadUrl").isNotEmpty());

        mockMvc.perform(delete("/api/documents/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_rejectsUnsupportedContentType() throws Exception {
        String token = newUserAccessToken();
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("type", "RESUME")
                        .param("label", "v1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotDownloadAnotherUsersDocument() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String id = uploadResume(ownerToken, "Owner's resume", "%PDF-1.4 owner bytes".getBytes());

        mockMvc.perform(get("/api/documents/" + id).header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());

        // Not in the stranger's list either.
        mockMvc.perform(get("/api/documents").header("Authorization", bearer(strangerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void userCannotDeleteAnotherUsersDocument() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String id = uploadResume(ownerToken, "Owner's resume", "%PDF-1.4 owner bytes".getBytes());

        mockMvc.perform(delete("/api/documents/" + id).header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());

        // Still there for the owner.
        mockMvc.perform(get("/api/documents/" + id).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
    }

    @Test
    void applicationCanBeLinkedToAResumeVersionTheCallerOwns_butNotToAnotherUsersDocument() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String resumeId = uploadResume(ownerToken, "Backend-focused v1", "%PDF-1.4 owner bytes".getBytes());

        MvcResult appResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Anthropic", "role": "Backend Engineer"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String applicationId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(patch("/api/applications/" + applicationId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeVersionId": "%s"}""".formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeVersionId").value(resumeId));

        // A stranger's own application can't be linked to the owner's resume version.
        MvcResult strangerAppResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Other Co", "role": "SWE"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String strangerAppId = objectMapper.readTree(strangerAppResult.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(patch("/api/applications/" + strangerAppId)
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeVersionId": "%s"}""".formatted(resumeId)))
                .andExpect(status().isNotFound());
    }
}
