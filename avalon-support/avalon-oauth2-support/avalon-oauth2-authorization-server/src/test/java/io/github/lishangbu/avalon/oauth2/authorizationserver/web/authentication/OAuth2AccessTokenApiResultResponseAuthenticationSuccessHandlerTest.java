package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.json.autoconfiguration.JacksonAutoConfiguration;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JacksonAutoConfiguration.class)
class OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandlerTest {

    @Autowired private JsonMapper jsonMapper;

    @Test
    void throwsWhenAuthenticationTypeIsInvalid() {
        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler handler =
                new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(jsonMapper);

        assertThrows(
                OAuth2AuthenticationException.class,
                () ->
                        handler.onAuthenticationSuccess(
                                new MockHttpServletRequest(),
                                new MockHttpServletResponse(),
                                new UsernamePasswordAuthenticationToken("user", "pwd")));
    }

    @Test
    void writesApiResultAndRecordsLog() throws Exception {
        CapturingRecorder recorder = new CapturingRecorder();
        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler handler =
                new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                        recorder, null, new Oauth2Properties(), jsonMapper);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "password");
        request.setParameter("username", "user");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("User-Agent", "JUnit");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, accessTokenAuthentication(Map.of()));

        assertEquals(200, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("access_token"));

        AuthenticationLogRecord record = recorder.getRecord();
        assertNotNull(record);
        assertEquals("user", record.username());
        assertEquals("client", record.clientId());
        assertEquals("password", record.grantType());
        assertEquals("10.0.0.1", record.ip());
        assertEquals("JUnit", record.userAgent());
    }

    @Test
    void usesAdditionalParametersAndAuthorizationServiceForUsername() throws Exception {
        CapturingRecorder recorder = new CapturingRecorder();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        OAuth2Authorization authorization =
                OAuth2Authorization.withRegisteredClient(registeredClient())
                        .principalName("auth-user")
                        .authorizationGrantType(
                                new org.springframework.security.oauth2.core.AuthorizationGrantType("password"))
                        .build();
        Mockito.when(authorizationService.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);

        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler handler =
                new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                        recorder, authorizationService, null, jsonMapper);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "192.168.0.1");

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put(OAuth2ParameterNames.GRANT_TYPE, "password");

        handler.onAuthenticationSuccess(
                request,
                new MockHttpServletResponse(),
                accessTokenAuthentication(additionalParameters));

        AuthenticationLogRecord record = recorder.getRecord();
        assertEquals("auth-user", record.username());
        assertEquals("192.168.0.1", record.ip());
    }

    @Test
    void handlesRecorderFailureGracefully() throws Exception {
        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler handler =
                new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                        record -> {
                            throw new IllegalStateException("boom");
                        },
                        null,
                        null,
                        jsonMapper);

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(), response, accessTokenAuthentication(Map.of()));

        assertEquals(200, response.getStatus());
    }

    @Test
    void appliesAccessTokenResponseCustomizer() throws Exception {
        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler handler =
                new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(jsonMapper);
        ReflectionTestUtils.setField(
                handler,
                "accessTokenResponseCustomizer",
                (Consumer<OAuth2AccessTokenAuthenticationContext>)
                        context ->
                                context.getAccessTokenResponse()
                                        .additionalParameters(Map.of("custom", "value")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(), response, accessTokenAuthentication(Map.of()));

        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertEquals("value", body.get("data").get("custom").asText());
    }

    @Test
    void resolveHelperMethodsCoverBranches() {
        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler handler =
                new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                        AuthenticationLogRecorder.noop(), null, null, jsonMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.1.1.1, 2.2.2.2");
        request.addHeader("X-Real-IP", "2.2.2.2");
        request.setRemoteAddr("127.0.0.1");

        String clientIp =
                ReflectionTestUtils.invokeMethod(handler, "resolveClientIp", request);
        assertEquals("1.1.1.1", clientIp);

        MockHttpServletRequest realIpRequest = new MockHttpServletRequest();
        realIpRequest.addHeader("X-Real-IP", "10.10.10.10");
        realIpRequest.setRemoteAddr("127.0.0.1");
        assertEquals(
                "10.10.10.10",
                ReflectionTestUtils.invokeMethod(handler, "resolveClientIp", realIpRequest));

        MockHttpServletRequest remoteAddrRequest = new MockHttpServletRequest();
        remoteAddrRequest.setRemoteAddr("127.0.0.1");
        assertEquals(
                "127.0.0.1",
                ReflectionTestUtils.invokeMethod(handler, "resolveClientIp", remoteAddrRequest));

        String grantType =
                ReflectionTestUtils.invokeMethod(
                        handler, "resolveGrantType", remoteAddrRequest, Map.of());
        assertNull(grantType);
        assertEquals(
                "username",
                ReflectionTestUtils.invokeMethod(handler, "resolveUsernameParameterName"));

        Object principal = new Object() {
            @Override
            public String toString() {
                return "principal";
            }
        };
        OAuth2AccessTokenAuthenticationToken authentication = accessTokenAuthentication(Map.of());

        String clientCredentialsUsername =
                ReflectionTestUtils.invokeMethod(
                        handler,
                        "resolveUsername",
                        principal,
                        remoteAddrRequest,
                        authentication,
                        AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        assertNull(clientCredentialsUsername);

        String resolved =
                ReflectionTestUtils.invokeMethod(
                        handler,
                        "resolveUsername",
                        principal,
                        remoteAddrRequest,
                        authentication,
                        "password");
        assertEquals("principal", resolved);

        Principal genericPrincipal = () -> "generic";
        resolved =
                ReflectionTestUtils.invokeMethod(
                        handler,
                        "resolveUsername",
                        genericPrincipal,
                        remoteAddrRequest,
                        authentication,
                        "password");
        assertEquals("generic", resolved);

        OAuth2ClientAuthenticationToken clientAuthenticationToken =
                new OAuth2ClientAuthenticationToken(
                        "client",
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                        "secret",
                        Map.of());
        resolved =
                ReflectionTestUtils.invokeMethod(
                        handler,
                        "resolveUsername",
                        clientAuthenticationToken,
                        remoteAddrRequest,
                        authentication,
                        "password");
        assertNull(resolved);

        OAuth2AccessTokenAuthenticationToken mockToken =
                Mockito.mock(OAuth2AccessTokenAuthenticationToken.class);
        Mockito.when(mockToken.getRegisteredClient()).thenReturn(null);
        String clientId =
                ReflectionTestUtils.invokeMethod(
                        handler,
                        "resolveClientId",
                        mockToken,
                        clientAuthenticationToken);
        assertEquals("client", clientId);
    }

    private static OAuth2AccessTokenAuthenticationToken accessTokenAuthentication(
            Map<String, Object> additionalParameters) {
        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token",
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        Set.of("read"));
        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        "refresh", Instant.now(), Instant.now().plusSeconds(120));
        RegisteredClient client = registeredClient();
        Authentication clientAuthentication =
                new OAuth2ClientAuthenticationToken(
                        client,
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                        "secret");
        return new OAuth2AccessTokenAuthenticationToken(
                client,
                clientAuthentication,
                accessToken,
                refreshToken,
                additionalParameters);
    }

    private static RegisteredClient registeredClient() {
        return RegisteredClient.withId("id")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        new org.springframework.security.oauth2.core.AuthorizationGrantType("password"))
                .scope("read")
                .tokenSettings(TokenSettings.builder().build())
                .build();
    }

    private static final class CapturingRecorder implements AuthenticationLogRecorder {
        private AuthenticationLogRecord record;

        @Override
        public void record(AuthenticationLogRecord record) {
            this.record = record;
        }

        private AuthenticationLogRecord getRecord() {
            return record;
        }
    }
}
