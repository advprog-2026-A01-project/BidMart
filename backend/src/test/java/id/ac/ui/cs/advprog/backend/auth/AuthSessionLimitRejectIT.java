package id.ac.ui.cs.advprog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        "auth.overflow-policy=REJECT"
})
class AuthSessionLimitRejectIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void second_login_rejected_with_429() throws Exception {
        final String regBody = mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("u_limit", "p"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final String verifyToken = om.readTree(regBody).get("verificationToken").asText();
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"" + verifyToken + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("u_limit", "p"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred("u_limit", "p"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("too_many_sessions"));

        assertThat(true).isTrue();
    }

    record Cred(String username, String password) {}
}