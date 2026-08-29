package com.example.anusha.job_trail.matching;

import com.example.anusha.job_trail.common.AbstractIntegrationTest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The one test the task calls for that covers Java -> Python -> stored
 * result end to end: real Postgres and MinIO (see AbstractIntegrationTest),
 * plus a real HTTP server standing in for ml-service (a JDK
 * {@link HttpServer}, not a mock of {@link MlServiceMatchClient} —
 * everything from the outbound HTTP call's request serialization through
 * the response deserialization and persistence is exercised for real).
 * Canned response bodies mirror ml-service's actual snake_case JSON
 * contract (see ml-service/app/schemas.py's ProfileResponse/ScoreResponse)
 * rather than this app's own camelCase shape, so a field-name mismatch
 * between the two sides would fail this test the same way it would in
 * production.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchingFlowIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger SCORE_CALL_COUNT = new AtomicInteger();
    private static final HttpServer FAKE_ML_SERVICE = startFakeMlService();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void mlServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ml-service.base-url", () -> "http://localhost:" + FAKE_ML_SERVICE.getAddress().getPort());
    }

    private static HttpServer startFakeMlService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/profile", exchange -> respond(exchange, """
                    {"profile": {"skills": ["python", "fastapi", "postgresql", "docker"], \
                    "years_experience": 5.0, "roles": ["Senior Backend Engineer"], \
                    "seniority": "senior", "summary": "Backend engineer."}, "confidence": 0.82}"""));
            server.createContext("/score", exchange -> {
                SCORE_CALL_COUNT.incrementAndGet();
                respond(exchange, """
                        {"match_pct": 0.75, "matched_skills": ["python", "fastapi"], \
                        "missing_skills": ["kubernetes"], "considered_skills": ["python", "fastapi", "kubernetes"]}""");
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String newUserAccessToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@jobtrail.dev";
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "correct-horse-battery"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void resumeParsesAndApplicationScoresAndCachesAgainstStoredResult() throws Exception {
        String token = newUserAccessToken();

        // 1. Upload a resume — plain text claiming to be a PDF, same trick
        // DocumentFlowIntegrationTest uses; Tika detects the real (plain
        // text) format from content, not the declared content-type.
        MockMultipartFile resumeFile = new MockMultipartFile("file", "resume.pdf", "application/pdf",
                "Senior Backend Engineer with 5 years of experience in Python, FastAPI, and PostgreSQL."
                        .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/documents")
                        .file(resumeFile)
                        .param("type", "RESUME")
                        .param("label", "Backend resume")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated());

        // 2. Parse it into a profile — real call to the fake ml-service.
        mockMvc.perform(post("/api/resume-profile/parse").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.skills").isArray())
                .andExpect(jsonPath("$.profile.skills[0]").value("python"))
                .andExpect(jsonPath("$.profile.seniority").value("senior"))
                .andExpect(jsonPath("$.confidence").value(0.82));

        // 3. Create an application with a job description to score against.
        MvcResult createResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Anthropic", "role": "Backend Engineer", \
                                "jobDescriptionText": "We need Python, FastAPI, and Kubernetes experience."}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String applicationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asString();

        // 4. Score it — a real call to the fake ml-service, result persisted.
        mockMvc.perform(post("/api/applications/" + applicationId + "/score").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").value(0.75))
                .andExpect(jsonPath("$.matchedSkills").isArray())
                .andExpect(jsonPath("$.matchedSkills[0]").value("python"))
                .andExpect(jsonPath("$.missingSkills[0]").value("kubernetes"))
                .andExpect(jsonPath("$.cached").value(false));
        assertThat(SCORE_CALL_COUNT.get()).isEqualTo(1);

        // 5. Score again, same resume profile and same JD text: the stored
        // result comes back as-is, no second call to ml-service.
        mockMvc.perform(post("/api/applications/" + applicationId + "/score").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").value(0.75))
                .andExpect(jsonPath("$.cached").value(true));
        assertThat(SCORE_CALL_COUNT.get()).isEqualTo(1);

        // 6. Change the JD text: the cache key no longer matches, so
        // scoring calls ml-service again.
        mockMvc.perform(patch("/api/applications/" + applicationId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobDescriptionText": "A completely different job description."}"""))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/applications/" + applicationId + "/score").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(false));
        assertThat(SCORE_CALL_COUNT.get()).isEqualTo(2);
    }

    @Test
    void scoringWithoutAJobDescriptionIsRejected() throws Exception {
        String token = newUserAccessToken();
        MvcResult createResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Anthropic", "role": "Backend Engineer"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String applicationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asString();

        mockMvc.perform(post("/api/applications/" + applicationId + "/score").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }
}
