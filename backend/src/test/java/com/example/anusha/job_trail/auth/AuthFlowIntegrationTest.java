package com.example.anusha.job_trail.auth;

import com.example.anusha.job_trail.common.AbstractIntegrationTest;
import com.example.anusha.job_trail.auth.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context and a real Postgres started by
 * Testcontainers (see AbstractIntegrationTest) — no mocks. Covers the whole
 * signup/login/refresh/logout lifecycle plus the protected-route rejection
 * cases called out as "done" criteria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@jobtrail.dev";
    }

    private String signupJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
    }

    private String refreshJson(String refreshToken) {
        return """
                {"refreshToken": "%s"}""".formatted(refreshToken);
    }

    @Test
    void signupThenLogin_bothReturnValidTokens() throws Exception {
        String email = uniqueEmail();

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "correct-horse-battery")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode signupBody = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        assertAccessTokenIsUsable(signupBody.get("accessToken").asString(), email);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "correct-horse-battery")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        assertAccessTokenIsUsable(loginBody.get("accessToken").asString(), email);

        // Login issues its own refresh token, independent of signup's. (Not
        // asserting the same for accessToken: JWTs are deterministic — same
        // claims issued in the same second sign to the same bytes — so
        // equality there would say nothing about whether login "worked".)
        assertThat(loginBody.get("refreshToken").asString()).isNotEqualTo(signupBody.get("refreshToken").asString());
    }

    private void assertAccessTokenIsUsable(String accessToken, String expectedEmail) throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(expectedEmail));
    }

    @Test
    void signup_rejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "correct-horse-battery")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "a-different-password")))
                .andExpect(status().isConflict());
    }

    @Test
    void login_rejectsWrongPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "correct-horse-battery")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_rejectsExpiredToken() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "ghost@jobtrail.dev")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_rotatesToken_andInvalidatesThePrevious() throws Exception {
        String email = uniqueEmail();
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "correct-horse-battery")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode signupBody = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String firstRefreshToken = signupBody.get("refreshToken").asString();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String secondRefreshToken = refreshBody.get("refreshToken").asString();

        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        // The just-rotated-away token must no longer work.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(firstRefreshToken)))
                .andExpect(status().isUnauthorized());

        // The current one still does.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(secondRefreshToken)))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_rejectsUnknownToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson("not-a-real-refresh-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTheToken() throws Exception {
        String email = uniqueEmail();
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "correct-horse-battery")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode signupBody = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String refreshToken = signupBody.get("refreshToken").asString();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isUnauthorized());
    }
}
