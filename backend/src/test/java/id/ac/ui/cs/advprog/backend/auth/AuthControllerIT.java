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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class AuthControllerIT {

    private static final String AUTHZ = "Authorization";
    private static final String AUTH_BEARER_PREFIX = "Bearer ";
    private static final String ROLE_BUYER = "BUYER";
    private static final String MFA_METHOD_EMAIL = "EMAIL";

    private static final String API_CAPTCHA = "/api/auth/captcha";
    private static final String API_REGISTER = "/api/auth/register";
    private static final String API_VERIFY_EMAIL = "/api/auth/verify-email";
    private static final String API_LOGIN = "/api/auth/login";
    private static final String API_REFRESH = "/api/auth/refresh";
    private static final String API_LOGOUT = "/api/auth/logout";
    private static final String API_ME = "/api/auth/me";
    private static final String API_ENABLE_EMAIL_2FA = "/api/auth/2fa/enable-email";
    private static final String API_VERIFY_2FA = "/api/auth/2fa/verify";

    private static final String JSON_OK = "$.ok";
    private static final String JSON_ERROR = "$.error";
    private static final String JSON_ACCESS_TOKEN = "$.accessToken";
    private static final String JSON_REFRESH_TOKEN = "$.refreshToken";
    private static final String JSON_USERNAME = "$.username";
    private static final String JSON_ROLE = "$.role";
    private static final String JSON_MFA_REQUIRED = "$.mfaRequired";
    private static final String JSON_METHOD = "$.method";

    private static final String FIELD_TOKEN = "token";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_ACCESS_TOKEN = "accessToken";
    private static final String FIELD_REFRESH_TOKEN = "refreshToken";
    private static final String FIELD_CHALLENGE_ID = "challengeId";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_CAPTCHA_ID = "captchaId";
    private static final String FIELD_CAPTCHA_ANSWER = "captchaAnswer";
    private static final String FIELD_DEV_ANSWER = "devAnswer";
    private static final String FIELD_ENABLED = "enabled";

    private static final String DEMO_VERIFY_CODE = "112233";
    private static final String DEMO_OTP_CODE = "445566";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Test
    void registerLoginMeLogoutFlow() throws Exception {
        final String username = "user1";
        final String password = "pass1";

        register(username, password)
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_OK).value(true));

        verifyEmailWithUsername(username, DEMO_VERIFY_CODE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_OK).value(true));

        final String accessToken = verifyDemoOtpAndGetAccessToken(username, password);
        assertThat(accessToken).isNotBlank();

        mvc.perform(get(API_ME).header(AUTHZ, authBearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_USERNAME).value(username))
                .andExpect(jsonPath(JSON_ROLE).value(ROLE_BUYER));

        logout(accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_OK).value(true));

        mvc.perform(get(API_ME).header(AUTHZ, authBearer(accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWrongPasswordReturns401() throws Exception {
        register("user2", "pass2")
                .andExpect(status().isCreated());

        verifyEmailWithUsername("user2", DEMO_VERIFY_CODE)
                .andExpect(status().isOk());

        login("user2", "WRONG")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(JSON_ERROR).value("invalid_credentials"));
    }

    @Test
    void refreshRotatesTokenAndOldRefreshInvalid() throws Exception {
        final String username = "user3";
        final String password = "pass3";

        register(username, password)
                .andExpect(status().isCreated());

        verifyEmailWithUsername(username, DEMO_VERIFY_CODE)
                .andExpect(status().isOk());

        final String refreshToken = verifyDemoOtpAndGetRefreshToken(username, password);
        assertThat(refreshToken).isNotBlank();

        final String refreshedBody = refresh(refreshToken)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String newAccess = jsonText(refreshedBody, FIELD_ACCESS_TOKEN);
        assertThat(newAccess).isNotBlank();

        mvc.perform(get(API_ME).header(AUTHZ, authBearer(newAccess)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_USERNAME).value(username));

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(JSON_ERROR).value("invalid_refresh_token"));
    }

    @Test
    void registerVerifyWithStaticCodeAndLoginWithStaticEmailOtp() throws Exception {
        final String username = "demo_static";
        final String password = "pass_static";

        register(username, password)
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_OK).value(true));

        verifyEmailWithUsername(username, DEMO_VERIFY_CODE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_OK).value(true));

        final String mfaBody = login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_MFA_REQUIRED).value(true))
                .andExpect(jsonPath(JSON_METHOD).value(MFA_METHOD_EMAIL))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String challengeId = jsonText(mfaBody, FIELD_CHALLENGE_ID);
        assertThat(challengeId).isNotBlank();

        final String tokenBody = verifyMfa(challengeId, DEMO_OTP_CODE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_ACCESS_TOKEN).isString())
                .andExpect(jsonPath(JSON_REFRESH_TOKEN).isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String accessToken = jsonText(tokenBody, FIELD_ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        enableEmail2fa(accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_OK).value(true));

        logout(accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_OK).value(true));

        final String mfaBody2 = login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_MFA_REQUIRED).value(true))
                .andExpect(jsonPath(JSON_METHOD).value(MFA_METHOD_EMAIL))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String challengeId2 = jsonText(mfaBody2, FIELD_CHALLENGE_ID);
        assertThat(challengeId2).isNotBlank();

        verifyMfa(challengeId2, DEMO_OTP_CODE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_ACCESS_TOKEN).isString())
                .andExpect(jsonPath(JSON_REFRESH_TOKEN).isString());
    }

    private ResultActions register(final String username, final String password) throws Exception {
        return mvc.perform(post(API_REGISTER)
                .contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(new Cred(username, password))));
    }

    private ResultActions verifyEmailWithUsername(final String username, final String code) throws Exception {
        return mvc.perform(post(API_VERIFY_EMAIL)
                .contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        FIELD_USERNAME, username,
                        FIELD_TOKEN, code
                ))));
    }

    private ResultActions login(final String username, final String password) throws Exception {
        final TestCaptcha captcha = issueCaptcha();

        return mvc.perform(post(API_LOGIN)
                .contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        FIELD_USERNAME, username,
                        FIELD_PASSWORD, password,
                        FIELD_CAPTCHA_ID, captcha.captchaId(),
                        FIELD_CAPTCHA_ANSWER, captcha.answer()
                ))));
    }

    private ResultActions refresh(final String refreshToken) throws Exception {
        return mvc.perform(post(API_REFRESH)
                .contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(FIELD_REFRESH_TOKEN, refreshToken))));
    }

    private ResultActions logout(final String accessToken) throws Exception {
        return mvc.perform(post(API_LOGOUT).header(AUTHZ, authBearer(accessToken)));
    }

    private ResultActions enableEmail2fa(final String accessToken) throws Exception {
        return mvc.perform(post(API_ENABLE_EMAIL_2FA).header(AUTHZ, authBearer(accessToken)));
    }

    private ResultActions verifyMfa(final String challengeId, final String code) throws Exception {
        return mvc.perform(post(API_VERIFY_2FA)
                .contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        FIELD_CHALLENGE_ID, challengeId,
                        FIELD_CODE, code
                ))));
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

    private String jsonText(final String body, final String fieldName) throws Exception {
        final JsonNode json = om.readTree(body);
        final JsonNode value = json.get(fieldName);
        assertThat(value).as(fieldName).isNotNull();

        final String text = value.asText();
        assertThat(text).isNotBlank();
        return text;
    }

    private String authBearer(final String accessToken) {
        return AUTH_BEARER_PREFIX + accessToken;
    }

    private String verifyDemoOtpAndGetAccessToken(final String username, final String password) throws Exception {
        final String mfaBody = login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_MFA_REQUIRED).value(true))
                .andExpect(jsonPath(JSON_METHOD).value(MFA_METHOD_EMAIL))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String challengeId = jsonText(mfaBody, FIELD_CHALLENGE_ID);
        assertThat(challengeId).isNotBlank();

        final String tokenBody = verifyMfa(challengeId, DEMO_OTP_CODE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_ACCESS_TOKEN).isString())
                .andExpect(jsonPath(JSON_REFRESH_TOKEN).isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return jsonText(tokenBody, FIELD_ACCESS_TOKEN);
    }

    private String verifyDemoOtpAndGetRefreshToken(final String username, final String password) throws Exception {
        final String mfaBody = login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_MFA_REQUIRED).value(true))
                .andExpect(jsonPath(JSON_METHOD).value(MFA_METHOD_EMAIL))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String challengeId = jsonText(mfaBody, FIELD_CHALLENGE_ID);
        assertThat(challengeId).isNotBlank();

        final String tokenBody = verifyMfa(challengeId, DEMO_OTP_CODE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_ACCESS_TOKEN).isString())
                .andExpect(jsonPath(JSON_REFRESH_TOKEN).isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return jsonText(tokenBody, FIELD_REFRESH_TOKEN);
    }

    record Cred(String username, String password) {}

    record TestCaptcha(String captchaId, String answer) {}
}