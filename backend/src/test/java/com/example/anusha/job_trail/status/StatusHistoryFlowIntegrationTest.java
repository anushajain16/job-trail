package com.example.anusha.job_trail.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context and the real (dockerized) Postgres
 * behind the "test" profile — no mocks. Covers the stage-change endpoint,
 * the ordered timeline it produces, and the auth boundary: one user's
 * application history must be invisible to another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatusHistoryFlowIntegrationTest {

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

    private void changeStage(String token, String id, String stage, org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        mockMvc.perform(patch("/api/applications/" + id + "/stage")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage": "%s"}""".formatted(stage)))
                .andExpect(expectedStatus);
    }

    @Test
    void creatingAnApplication_writesTheInitialSavedHistoryRow() throws Exception {
        String token = newUserAccessToken();
        String id = createApplication(token);

        mockMvc.perform(get("/api/applications/" + id + "/history").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stage").value("SAVED"));
    }

    @Test
    void stageChange_writesExactlyOneHistoryRow_andSyncsCurrentStage() throws Exception {
        String token = newUserAccessToken();
        String id = createApplication(token);

        mockMvc.perform(patch("/api/applications/" + id + "/stage")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage": "APPLIED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStage").value("APPLIED"));

        mockMvc.perform(get("/api/applications/" + id + "/history").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stage").value("SAVED"))
                .andExpect(jsonPath("$[1].stage").value("APPLIED"));
    }

    @Test
    void history_returnsTheFullOrderedTimeline() throws Exception {
        String token = newUserAccessToken();
        String id = createApplication(token);

        changeStage(token, id, "APPLIED", status().isOk());
        changeStage(token, id, "SCREEN", status().isOk());
        changeStage(token, id, "INTERVIEW", status().isOk());

        mockMvc.perform(get("/api/applications/" + id + "/history").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].stage").value("SAVED"))
                .andExpect(jsonPath("$[1].stage").value("APPLIED"))
                .andExpect(jsonPath("$[2].stage").value("SCREEN"))
                .andExpect(jsonPath("$[3].stage").value("INTERVIEW"));
    }

    @Test
    void stageChange_rejectsTransitionToTheSameStage() throws Exception {
        String token = newUserAccessToken();
        String id = createApplication(token);

        changeStage(token, id, "SAVED", status().isBadRequest());
    }

    @Test
    void stageChange_rejectsUnknownStageValue() throws Exception {
        String token = newUserAccessToken();
        String id = createApplication(token);

        mockMvc.perform(patch("/api/applications/" + id + "/stage")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage": "NOT_A_STAGE"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotChangeStage_orReadHistory_forAnotherUsersApplication() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String id = createApplication(ownerToken);

        changeStage(strangerToken, id, "APPLIED", status().isNotFound());

        mockMvc.perform(get("/api/applications/" + id + "/history").header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());

        // The owner's timeline is untouched.
        mockMvc.perform(get("/api/applications/" + id + "/history").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void protectedRoutes_rejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/applications/" + UUID.randomUUID() + "/history"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/applications/" + UUID.randomUUID() + "/stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage": "APPLIED"}"""))
                .andExpect(status().isUnauthorized());
    }
}
