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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "auth.max-sessions-per-user=1",
        "auth.overflow-policy=REVOKE_OLDEST"
})
class AuthSessionLimitRevokeOldestIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void second_login_revokes_oldest_session() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("u_revoke", "p"))))
                .andExpect(status().isCreated());

        String login1 = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("u_revoke", "p"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String login2 = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("u_revoke", "p"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode j1 = om.readTree(login1);
        JsonNode j2 = om.readTree(login2);
        String t1 = j1.get("accessToken").asText();
        String t2 = j2.get("accessToken").asText();
        assertThat(t1).isNotEqualTo(t2);

        // old token should be unauthorized
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + t1))
                .andExpect(status().isUnauthorized());

        // new token should work
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + t2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("u_revoke"));
    }

    record Cred(String username, String password) {}
}