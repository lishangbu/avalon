package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.lishangbu.avalon.json.autoconfiguration.JacksonAutoConfiguration;
import io.github.lishangbu.avalon.json.util.JsonUtils;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JacksonAutoConfiguration.class)
class OAuth2ErrorApiResultAuthenticationFailureHandlerTest {

    @Test
    void writesDescriptionForOAuth2Error() throws Exception {
        OAuth2ErrorApiResultAuthenticationFailureHandler handler =
                new OAuth2ErrorApiResultAuthenticationFailureHandler(
                        AuthenticationLogRecorder.noop(), new Oauth2Properties());
        OAuth2AuthenticationException exception =
                new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_request", "detail", null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        JsonNode body = JsonUtils.getInstance().readTree(response.getContentAsString());
        assertEquals("detail", body.get("errorMessage").asText());
    }

    @Test
    void sanitizesInvalidGrantDescriptions() throws Exception {
        OAuth2ErrorApiResultAuthenticationFailureHandler handler =
                new OAuth2ErrorApiResultAuthenticationFailureHandler(
                        AuthenticationLogRecorder.noop(), null);
        OAuth2AuthenticationException exception =
                new OAuth2AuthenticationException(
                        new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "invalid_grant: bad", null));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);

        JsonNode body = JsonUtils.getInstance().readTree(response.getContentAsString());
        assertEquals("bad", body.get("errorMessage").asText());

        OAuth2AuthenticationException plainInvalidGrant =
                new OAuth2AuthenticationException(
                        new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "invalid_grant", null));
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response2, plainInvalidGrant);

        JsonNode body2 = JsonUtils.getInstance().readTree(response2.getContentAsString());
        assertEquals("Bad Request", body2.get("errorMessage").asText());
    }

    @Test
    void handlesNonOAuth2ExceptionAndExtractsClientId() throws Exception {
        OAuth2ErrorApiResultAuthenticationFailureHandler handler =
                new OAuth2ErrorApiResultAuthenticationFailureHandler(
                        AuthenticationLogRecorder.noop(), null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        String credentials = Base64.getEncoder().encodeToString("client:secret".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

        JsonNode body = JsonUtils.getInstance().readTree(response.getContentAsString());
        assertEquals("Bad credentials", body.get("errorMessage").asText());
    }

    @Test
    void resolveHelperMethodsCoverBranches() throws Exception {
        OAuth2ErrorApiResultAuthenticationFailureHandler handler =
                new OAuth2ErrorApiResultAuthenticationFailureHandler(
                        AuthenticationLogRecorder.noop(), null);

        assertNull(ReflectionTestUtils.invokeMethod(handler, "sanitizeDescription", "invalid_grant"));
        assertEquals(
                "reason",
                ReflectionTestUtils.invokeMethod(handler, "sanitizeDescription", "[invalid_grant] reason"));
        assertEquals(
                "reason",
                ReflectionTestUtils.invokeMethod(handler, "sanitizeDescription", "invalid_grant:reason"));
        assertEquals(
                "reason",
                ReflectionTestUtils.invokeMethod(handler, "sanitizeDescription", "invalid_grantreason"));
        assertEquals(
                "detail",
                ReflectionTestUtils.invokeMethod(handler, "sanitizeDescription", "detail"));
        assertEquals(
                "username",
                ReflectionTestUtils.invokeMethod(handler, "resolveUsernameParameterName"));
        ReflectionTestUtils.invokeMethod(handler, "resolveError", new Object[] {null});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("client_id", "param-client");
        assertEquals(
                "param-client",
                ReflectionTestUtils.invokeMethod(handler, "resolveClientId", request));

        MockHttpServletRequest badHeaderRequest = new MockHttpServletRequest();
        badHeaderRequest.addHeader("Authorization", "Basic ***");
        assertNull(
                ReflectionTestUtils.invokeMethod(handler, "resolveClientId", badHeaderRequest));

        MockHttpServletRequest emptyHeaderRequest = new MockHttpServletRequest();
        emptyHeaderRequest.addHeader("Authorization", "Basic ");
        assertNull(
                ReflectionTestUtils.invokeMethod(handler, "resolveClientId", emptyHeaderRequest));

        MockHttpServletRequest noColonRequest = new MockHttpServletRequest();
        String noColon = Base64.getEncoder().encodeToString("clientsecret".getBytes(StandardCharsets.UTF_8));
        noColonRequest.addHeader("Authorization", "Basic " + noColon);
        assertNull(
                ReflectionTestUtils.invokeMethod(handler, "resolveClientId", noColonRequest));

        MockHttpServletRequest forwarded = new MockHttpServletRequest();
        forwarded.addHeader("X-Forwarded-For", "1.1.1.1, 2.2.2.2");
        assertEquals(
                "1.1.1.1",
                ReflectionTestUtils.invokeMethod(handler, "resolveClientIp", forwarded));

        MockHttpServletRequest realIp = new MockHttpServletRequest();
        realIp.addHeader("X-Real-IP", "9.9.9.9");
        assertEquals(
                "9.9.9.9",
                ReflectionTestUtils.invokeMethod(handler, "resolveClientIp", realIp));

        MockHttpServletRequest remoteAddr = new MockHttpServletRequest();
        remoteAddr.setRemoteAddr("127.0.0.1");
        assertEquals(
                "127.0.0.1",
                ReflectionTestUtils.invokeMethod(handler, "resolveClientIp", remoteAddr));

        Oauth2Properties properties = new Oauth2Properties();
        properties.setUsernameParameterName("   ");
        OAuth2ErrorApiResultAuthenticationFailureHandler handlerWithProps =
                new OAuth2ErrorApiResultAuthenticationFailureHandler(
                        AuthenticationLogRecorder.noop(), properties);
        assertEquals(
                "username",
                ReflectionTestUtils.invokeMethod(handlerWithProps, "resolveUsernameParameterName"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        ReflectionTestUtils.invokeMethod(
                handler, "writeFailedResponse", response, "custom", null);
        assertNotNull(response.getContentAsString());
    }
}
