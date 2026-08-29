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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context and a real Postgres started by
 * Testcontainers (see AbstractIntegrationTest). Covers the nested
 * list/create endpoints, the flat update/delete endpoints, the
 * cascade-on-application-delete, and the auth boundary: one user's rounds
 * must be invisible (and unreachable) to another, whether reached through
 * the owning application or by the round's own id.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InterviewRoundFlowIntegrationTest extends AbstractIntegrationTest {

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

    private String createRound(String token, String applicationId, String roundType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "%s"}""".formatted(roundType)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    @Test
    void createAndList_roundtripsAllFields_orderedByScheduledAt() throws Exception {
        String token = newUserAccessToken();
        String applicationId = createApplication(token);

        mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Technical R2", "scheduledAt": "2026-09-05T15:00:00Z",
                                 "interviewerName": "Jordan", "questionsAsked": "reverse a linked list",
                                 "notes": "went well", "reflection": "explain trade-offs sooner next time"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.roundType").value("Technical R2"))
                .andExpect(jsonPath("$.interviewerName").value("Jordan"))
                .andExpect(jsonPath("$.reflection").value("explain trade-offs sooner next time"));

        mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Screen", "scheduledAt": "2026-09-01T15:00:00Z"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roundType").value("Screen"))
                .andExpect(jsonPath("$[1].roundType").value("Technical R2"));
    }

    @Test
    void update_editsFieldsInPlace_leavingOmittedFieldsUntouched() throws Exception {
        String token = newUserAccessToken();
        String applicationId = createApplication(token);
        String roundId = createRound(token, applicationId, "Screen");

        mockMvc.perform(patch("/api/interviews/" + roundId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reflection": "should have asked about team structure"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundType").value("Screen"))
                .andExpect(jsonPath("$.reflection").value("should have asked about team structure"));

        mockMvc.perform(patch("/api/interviews/" + roundId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Technical R1"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundType").value("Technical R1"))
                .andExpect(jsonPath("$.reflection").value("should have asked about team structure"));
    }

    @Test
    void delete_removesTheRound() throws Exception {
        String token = newUserAccessToken();
        String applicationId = createApplication(token);
        String roundId = createRound(token, applicationId, "Screen");

        mockMvc.perform(delete("/api/interviews/" + roundId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deletingAnApplication_cascadesToItsRounds() throws Exception {
        String token = newUserAccessToken();
        String applicationId = createApplication(token);
        String roundId = createRound(token, applicationId, "Screen");

        mockMvc.perform(delete("/api/applications/" + applicationId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/interviews/" + roundId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Screen"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotReadOrCreateRounds_forAnotherUsersApplication() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String applicationId = createApplication(ownerToken);

        mockMvc.perform(get("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Screen"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotUpdateOrDelete_anotherUsersRound() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String applicationId = createApplication(ownerToken);
        String roundId = createRound(ownerToken, applicationId, "Screen");

        mockMvc.perform(patch("/api/interviews/" + roundId)
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reflection": "not mine to edit"}"""))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/interviews/" + roundId).header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());

        // Still there, unedited, for the owner.
        mockMvc.perform(get("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reflection").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void create_rejectsBlankRoundType() throws Exception {
        String token = newUserAccessToken();
        String applicationId = createApplication(token);

        mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": ""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedRoutes_rejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/applications/" + UUID.randomUUID() + "/interviews"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/interviews/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
