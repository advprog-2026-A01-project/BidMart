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
        "auth.overflow-policy=REJECT"
})
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class AuthSessionLimitRejectIT {

    private static final String API_CAPTCHA = "/api/auth/captcha";
    private static final String API_REGISTER = "/api/auth/register";
    private static final String API_VERIFY_EMAIL = "/api/auth/verify-email";
    private static final String API_LOGIN = "/api/auth/login";
    private static final String API_VERIFY_2FA = "/api/auth/2fa/verify";

    private static final String USER = "u_limit";
    private static final String VERIFY_CODE = "112233";
    private static final String OTP_CODE = "445566";

    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_TOKEN = "token";
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
    void second_login_rejected_with_429() throws Exception {
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

        final JsonNode j1 = om.readTree(login1);
        final String challengeId1 = j1.get(FIELD_CHALLENGE_ID).asText();
        assertThat(challengeId1).isNotBlank();

        mvc.perform(post(API_VERIFY_2FA)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_CHALLENGE_ID, challengeId1,
                                FIELD_CODE, OTP_CODE
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());

        final TestCaptcha captcha2 = issueCaptcha();

        mvc.perform(post(API_LOGIN)
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                FIELD_USERNAME, USER,
                                FIELD_PASSWORD, "p",
                                FIELD_CAPTCHA_ID, captcha2.captchaId(),
                                FIELD_CAPTCHA_ANSWER, captcha2.answer()
                        ))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("too_many_sessions"));
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