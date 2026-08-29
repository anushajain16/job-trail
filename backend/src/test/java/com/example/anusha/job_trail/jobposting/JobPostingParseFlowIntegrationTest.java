package com.example.anusha.job_trail.jobposting;

import com.example.anusha.job_trail.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The "done when" of this whole feature: nothing in the test profile runs
 * ml-service (see AbstractIntegrationTest / application.yml's test
 * profile), so every one of these calls exercises the real graceful
 * fallback path against an actually-unreachable ml-service — not a mock
 * standing in for one being down.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JobPostingParseFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    void parseFallsBackToManualEntryWhenMlServiceIsUnreachable() throws Exception {
        String token = newUserAccessToken();

        MvcResult result = mockMvc.perform(post("/api/job-postings/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "https://boards.example.com/job/1"}"""))
                // The documented failure path is a normal 200, not an error —
                // manual entry must still work with the ML service off.
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("available").asBoolean()).isFalse();
        assertThat(body.get("parsed").isNull()).isTrue();
        assertThat(body.get("message").asString()).isNotBlank();
    }

    @Test
    void parseRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/job-postings/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "https://boards.example.com/job/1"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void parseRejectsBlankUrl() throws Exception {
        String token = newUserAccessToken();

        mockMvc.perform(post("/api/job-postings/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": ""}"""))
                .andExpect(status().isBadRequest());
    }
}
