package com.example.anusha.job_trail.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Only covers what's verifiable without a real Google/GitHub round trip —
 * an unsupported provider and a token that fails verification locally (a
 * malformed Google ID token never gets as far as a network call: JWT
 * parsing fails first). Exercising a genuinely valid provider token would
 * need either a live account or a fake {@code OAuthProviderClient}, neither
 * of which this suite's plain-integration style (no mocking framework, real
 * app context) is set up for.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthLoginFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String tokenJson(String token) {
        return """
                {"token": "%s"}""".formatted(token);
    }

    @Test
    void oauthLogin_rejectsUnsupportedProvider() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson("irrelevant")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oauthLogin_rejectsBlankToken() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oauthLogin_rejectsGoogleTokenThatFailsLocalVerification() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson("not-a-real-jwt")))
                .andExpect(status().isUnauthorized());
    }
}
