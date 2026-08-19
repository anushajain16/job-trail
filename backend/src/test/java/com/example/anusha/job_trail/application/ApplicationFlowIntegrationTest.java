package com.example.anusha.job_trail.application;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context and the real (dockerized) Postgres
 * behind the "test" profile — no mocks. Covers full CRUD, validation
 * failures, and the auth-boundary requirement: one user's applications must
 * be invisible and unmodifiable to another, even by guessing a valid id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@jobtrail.dev";
    }

    /** Signs up a fresh user and returns their access token. */
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

    private String createApplicationJson(String company, String role) {
        return """
                {"company": "%s", "role": "%s"}""".formatted(company, role);
    }

    private String createApplication(String token, String company, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createApplicationJson(company, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        String token = newUserAccessToken();

        String id = createApplication(token, "Anthropic", "Backend Engineer");

        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Anthropic"))
                .andExpect(jsonPath("$.role").value("Backend Engineer"));

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch("/api/applications/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"location": "Remote", "salaryMin": 100000, "salaryMax": 150000}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Anthropic"))
                .andExpect(jsonPath("$.location").value("Remote"))
                .andExpect(jsonPath("$.salaryMin").value(100000))
                .andExpect(jsonPath("$.salaryMax").value(150000));

        mockMvc.perform(delete("/api/applications/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_rejectsMissingRequiredFields_with400AndFieldErrors() throws Exception {
        String token = newUserAccessToken();

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("company")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("role")));
    }

    @Test
    void create_rejectsSalaryMinGreaterThanSalaryMax() throws Exception {
        String token = newUserAccessToken();

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Anthropic", "role": "SWE", "salaryMin": 200000, "salaryMax": 100000}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_rejectsInvalidLink() throws Exception {
        String token = newUserAccessToken();
        String id = createApplication(token, "Anthropic", "SWE");

        mockMvc.perform(patch("/api/applications/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "not-a-url"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_onlyReturnsCallersOwnApplications() throws Exception {
        String tokenA = newUserAccessToken();
        String tokenB = newUserAccessToken();
        createApplication(tokenA, "Company A", "Role A");
        createApplication(tokenB, "Company B", "Role B");

        mockMvc.perform(get("/api/applications").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].company").value("Company A"));
    }

    @Test
    void userCannotReadAnotherUsersApplication() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String id = createApplication(ownerToken, "Anthropic", "SWE");

        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotUpdateAnotherUsersApplication() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String id = createApplication(ownerToken, "Anthropic", "SWE");

        mockMvc.perform(patch("/api/applications/" + id)
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Hijacked"}"""))
                .andExpect(status().isNotFound());

        // The owner's data is untouched.
        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Anthropic"));
    }

    @Test
    void userCannotDeleteAnotherUsersApplication() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        String id = createApplication(ownerToken, "Anthropic", "SWE");

        mockMvc.perform(delete("/api/applications/" + id).header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());

        // Still there for the owner.
        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRoutes_rejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createApplicationJson("Anthropic", "SWE")))
                .andExpect(status().isUnauthorized());
    }
}
