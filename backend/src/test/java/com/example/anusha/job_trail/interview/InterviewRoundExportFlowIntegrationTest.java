package com.example.anusha.job_trail.interview;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context and a real Postgres started by
 * Testcontainers. Covers the CSV shape (one row per round, company/role
 * carried along even though a round doesn't own those columns itself) and
 * the auth boundary: the export must only ever contain rounds on the
 * caller's own applications.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InterviewRoundExportFlowIntegrationTest extends AbstractIntegrationTest {

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

    private String createApplication(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Anthropic", "role": "Backend Engineer"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private void createRound(String token, String applicationId) throws Exception {
        mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Screen", "interviewerName": "Jordan",
                                 "reflection": "should have asked about team, size"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void export_streamsCsvWithOneRowPerRound_carryingTheApplicationsCompanyAndRole() throws Exception {
        String token = newUserAccessToken();
        String applicationId = createApplication(token);
        createRound(token, applicationId);

        MvcResult result = mockMvc.perform(get("/api/interviews/export").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"jobtrail-interview-rounds-export.csv\""))
                .andReturn();

        String[] lines = result.getResponse().getContentAsString().split("\r\n");
        assertThat(lines[0]).isEqualTo("Company,Role,Round Type,Scheduled At,Interviewer,Questions Asked,Notes,Reflection");
        assertThat(lines).hasSize(2);
        assertThat(lines[1]).isEqualTo(
                "Anthropic,Backend Engineer,Screen,,Jordan,,,\"should have asked about team, size\"");
    }

    @Test
    void export_onlyContainsRoundsOnTheCallersOwnApplications() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String applicationId = createApplication(ownerToken);
        createRound(ownerToken, applicationId);

        MvcResult result = mockMvc.perform(get("/api/interviews/export").header("Authorization", bearer(strangerToken)))
                .andExpect(status().isOk())
                .andReturn();

        String[] lines = result.getResponse().getContentAsString().split("\r\n");
        assertThat(lines).hasSize(1); // header only
    }

    @Test
    void export_rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/interviews/export")).andExpect(status().isUnauthorized());
    }
}
