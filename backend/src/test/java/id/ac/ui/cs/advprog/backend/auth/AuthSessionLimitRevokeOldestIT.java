package id.ac.ui.cs.advprog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        "auth.overflow-policy=REVOKE_OLDEST"
})
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class AuthSessionLimitRevokeOldestIT {

    private static final String AUTHZ = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String USER = "u_revoke";
    private static final String VERIFY_CODE = "112233";
    private static final String OTP_CODE = "445566";
    private static final String FIELD_CHALLENGE_ID = "challengeId";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void second_login_revokes_oldest_session() throws Exception {
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

        final String challenge1 = om.readTree(login1).get(FIELD_CHALLENGE_ID).asText();
        assertThat(challenge1).isNotBlank();

        final String tokenBody1 = mvc.perform(post("/api/auth/2fa/verify")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_CHALLENGE_ID, challenge1,
                                "code", OTP_CODE
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final String t1 = om.readTree(tokenBody1).get("accessToken").asText();
        assertThat(t1).isNotBlank();

        final String login2 = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(USER, "p"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn().getResponse().getContentAsString();

        final String challenge2 = om.readTree(login2).get(FIELD_CHALLENGE_ID).asText();
        assertThat(challenge2).isNotBlank();

        final String tokenBody2 = mvc.perform(post("/api/auth/2fa/verify")
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_CHALLENGE_ID, challenge2,
                                "code", OTP_CODE
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final String t2 = om.readTree(tokenBody2).get("accessToken").asText();
        assertThat(t2).isNotBlank();
        assertThat(t1).isNotEqualTo(t2);

        mvc.perform(get("/api/auth/me").header(AUTHZ, BEARER + t1))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/auth/me").header(AUTHZ, BEARER + t2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USER));
    }

    record Cred(String username, String password) {}
}