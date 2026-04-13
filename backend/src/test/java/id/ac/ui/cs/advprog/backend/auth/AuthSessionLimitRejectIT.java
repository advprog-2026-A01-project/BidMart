package id.ac.ui.cs.advprog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class AuthSessionLimitRejectIT {

    private static final String USER = "u_limit";
    private static final String VERIFY_CODE = "112233";
    private static final String OTP_CODE = "445566";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void second_login_rejected_with_429() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(USER, "p"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/verify-email")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "username", USER,
                                "token", VERIFY_CODE
                        ))))
                .andExpect(status().isOk());

        final String login1 = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(USER, "p"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn().getResponse().getContentAsString();

        final JsonNode j1 = om.readTree(login1);
        final String challengeId1 = j1.get("challengeId").asText();
        assertThat(challengeId1).isNotBlank();

        mvc.perform(post("/api/auth/2fa/verify")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "challengeId", challengeId1,
                                "code", OTP_CODE
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(USER, "p"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("too_many_sessions"));
    }

    record Cred(String username, String password) {}
}