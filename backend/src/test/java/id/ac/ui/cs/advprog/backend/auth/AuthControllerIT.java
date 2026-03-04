package id.ac.ui.cs.advprog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void register_login_me_logout_flow() throws Exception {
        final String username = "user1";
        final String password = "pass1";

        // register
        mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(username, password))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true));

        // login
        String loginBody = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn().getResponse().getContentAsString();

        JsonNode loginJson = om.readTree(loginBody);
        String accessToken = loginJson.get("accessToken").asText();

        // me
        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("BUYER"));

        // logout
        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        // me after logout => 401
        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_wrong_password_returns_401() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("user2", "pass2"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("user2", "WRONG"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void refresh_rotates_token_and_old_refresh_invalid() throws Exception {
        final String username = "user3";
        final String password = "pass3";

        mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(username, password))))
                .andExpect(status().isCreated());

        String loginBody = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode loginJson = om.readTree(loginBody);
        String refreshToken = loginJson.get("refreshToken").asText();

        // refresh once
        String refreshedBody = mvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode refreshedJson = om.readTree(refreshedBody);
        String newAccess = refreshedJson.get("accessToken").asText();
        assertThat(newAccess).isNotBlank();

        // /me works with new access
        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        // old refresh token should now be invalid (because repo deletes old session row)
        mvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_refresh_token"));
    }

    record Cred(String username, String password) {}
}