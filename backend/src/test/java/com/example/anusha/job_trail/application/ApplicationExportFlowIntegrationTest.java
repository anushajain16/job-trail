package com.example.anusha.job_trail.application;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context and a real Postgres started by
 * Testcontainers. Covers the CSV shape (header, one row per application,
 * commas inside a field correctly quoted), the "applied date" column's
 * derivation from status_history, and the auth boundary: the export must
 * only ever contain the caller's own applications.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationExportFlowIntegrationTest extends AbstractIntegrationTest {

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

    private String createApplication(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    @Test
    void export_streamsCsvWithOneRowPerApplication_quotingFieldsThatNeedIt() throws Exception {
        String token = newUserAccessToken();
        createApplication(token, """
                {"company": "Anthropic", "role": "Backend Engineer", "location": "Remote",
                 "salaryMin": 150000, "salaryMax": 200000, "source": "Referral",
                 "notes": "Great, fast-moving team; asked about my \\"biggest failure\\""}""");
        createApplication(token, """
                {"company": "Other Co", "role": "SWE"}""");

        MvcResult result = mockMvc.perform(get("/api/applications/export").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"jobtrail-export.csv\""))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        String[] lines = csv.split("\r\n");
        assertThat(lines[0]).isEqualTo(
                "Company,Role,Location,Salary Range,Source,Current Stage,Applied Date,Deadline,Notes");
        assertThat(lines).hasSize(3); // header + 2 rows
        // Newest first, same ordering as the paginated list.
        assertThat(lines[1]).startsWith("Other Co,SWE,,,,SAVED,,,");
        assertThat(lines[2]).isEqualTo(
                "Anthropic,Backend Engineer,Remote,150000-200000,Referral,SAVED,,,"
                        + "\"Great, fast-moving team; asked about my \"\"biggest failure\"\"\"");
    }

    @Test
    void export_appliedDateColumn_reflectsTheFirstAppliedStageTransition() throws Exception {
        String token = newUserAccessToken();
        String id = createApplication(token, """
                {"company": "Anthropic", "role": "Backend Engineer"}""");

        mockMvc.perform(patch("/api/applications/" + id + "/stage")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage": "APPLIED"}"""))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/applications/export").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        String[] lines = result.getResponse().getContentAsString().split("\r\n");
        assertThat(lines[1]).contains(",APPLIED," + java.time.LocalDate.now(java.time.ZoneOffset.UTC) + ",");
    }

    @Test
    void export_onlyContainsTheCallersOwnApplications() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        createApplication(ownerToken, """
                {"company": "Anthropic", "role": "Backend Engineer"}""");

        MvcResult result = mockMvc.perform(get("/api/applications/export").header("Authorization", bearer(strangerToken)))
                .andExpect(status().isOk())
                .andReturn();

        String[] lines = result.getResponse().getContentAsString().split("\r\n");
        assertThat(lines).hasSize(1); // header only
    }

    @Test
    void export_rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/applications/export")).andExpect(status().isUnauthorized());
    }
}
