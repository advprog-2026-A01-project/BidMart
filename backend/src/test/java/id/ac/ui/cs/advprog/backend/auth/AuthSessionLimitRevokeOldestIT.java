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

    private static final String API_CAPTCHA = "/api/auth/captcha";
    private static final String API_REGISTER = "/api/auth/register";
    private static final String API_VERIFY_EMAIL = "/api/auth/verify-email";
    private static final String API_LOGIN = "/api/auth/login";
    private static final String API_VERIFY_2FA = "/api/auth/2fa/verify";
    private static final String API_ME = "/api/auth/me";

    private static final String USER = "u_revoke";
    private static final String VERIFY_CODE = "112233";
    private static final String OTP_CODE = "445566";

    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_TOKEN = "token";
    private static final String FIELD_ACCESS_TOKEN = "accessToken";
    private static final String FIELD_CHALLENGE_ID = "challengeId";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_CAPTCHA_ID = "captchaId";
    private static final String FIELD_CAPTCHA_ANSWER = "captchaAnswer";
    private static final String FIELD_DEV_ANSWER = "devAnswer";
    private static final String FIELD_ENABLED = "enabled";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Test
    void second_login_revokes_oldest_session() throws Exception {
        mvc.perform(post(API_REGISTER)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(new Cred(USER, "p"))))
                .andExpect(status().isCreated());

        mvc.perform(post(API_VERIFY_EMAIL)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_USERNAME, USER,
                                FIELD_TOKEN, VERIFY_CODE
                        ))))
                .andExpect(status().isOk());

        final TestCaptcha captcha1 = issueCaptcha();

        final String login1 = mvc.perform(post(API_LOGIN)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_USERNAME, USER,
                                FIELD_PASSWORD, "p",
                                FIELD_CAPTCHA_ID, captcha1.captchaId(),
                                FIELD_CAPTCHA_ANSWER, captcha1.answer()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String challenge1 = om.readTree(login1).get(FIELD_CHALLENGE_ID).asText();
        assertThat(challenge1).isNotBlank();

        final String tokenBody1 = mvc.perform(post(API_VERIFY_2FA)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_CHALLENGE_ID, challenge1,
                                FIELD_CODE, OTP_CODE
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String t1 = om.readTree(tokenBody1).get(FIELD_ACCESS_TOKEN).asText();
        assertThat(t1).isNotBlank();

        final TestCaptcha captcha2 = issueCaptcha();

        final String login2 = mvc.perform(post(API_LOGIN)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_USERNAME, USER,
                                FIELD_PASSWORD, "p",
                                FIELD_CAPTCHA_ID, captcha2.captchaId(),
                                FIELD_CAPTCHA_ANSWER, captcha2.answer()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String challenge2 = om.readTree(login2).get(FIELD_CHALLENGE_ID).asText();
        assertThat(challenge2).isNotBlank();

        final String tokenBody2 = mvc.perform(post(API_VERIFY_2FA)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_CHALLENGE_ID, challenge2,
                                FIELD_CODE, OTP_CODE
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String t2 = om.readTree(tokenBody2).get(FIELD_ACCESS_TOKEN).asText();
        assertThat(t2).isNotBlank();
        assertThat(t1).isNotEqualTo(t2);

        mvc.perform(get(API_ME).header(AUTHZ, BEARER + t1))
                .andExpect(status().isUnauthorized());

        mvc.perform(get(API_ME).header(AUTHZ, BEARER + t2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USER));
    }

    private TestCaptcha issueCaptcha() throws Exception {
        final String body = mvc.perform(get(API_CAPTCHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + FIELD_ENABLED).value(true))
                .andExpect(jsonPath("$." + FIELD_CAPTCHA_ID).isString())
                .andExpect(jsonPath("$." + FIELD_DEV_ANSWER).isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final JsonNode json = om.readTree(body);
        final String captchaId = json.get(FIELD_CAPTCHA_ID).asText();
        final String answer = json.get(FIELD_DEV_ANSWER).asText();

        assertThat(captchaId).isNotBlank();
        assertThat(answer).isNotBlank();

        return new TestCaptcha(captchaId, answer);
    }

    record Cred(String username, String password) {}

    record TestCaptcha(String captchaId, String answer) {}
}