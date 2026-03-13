package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.json.autoconfiguration.JacksonAutoConfiguration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JacksonAutoConfiguration.class)
class AuthorizationEndpointHandlersTest {

    @Autowired private JsonMapper jsonMapper;

    @Test
    void authorizationEndpointErrorResponseWritesApiResult() throws Exception {
        AuthorizationEndpointErrorResponseHandler handler =
                new AuthorizationEndpointErrorResponseHandler(jsonMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException("unauthorized"));

        assertEquals(401, response.getStatus());
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertEquals("unauthorized", body.get("errorMessage").asText());
    }

    @Test
    void authorizationEndpointResponseHandlerWritesTokenResponse() throws Exception {
        AuthorizationEndpointResponseHandler handler = new AuthorizationEndpointResponseHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AccessTokenAuthenticationToken authentication = accessTokenAuthentication();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(200, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("access_token"));
        assertTrue(body.contains("token_type"));
    }

    @Test
    void authorizationEndpointResponseHandlerAppliesCustomizer() throws Exception {
        AuthorizationEndpointResponseHandler handler = new AuthorizationEndpointResponseHandler();
        handler.setAccessTokenResponseCustomizer(
                context -> context.getAccessTokenResponse()
                        .additionalParameters(Map.of("custom", "value")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(), response, accessTokenAuthentication());

        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        JsonNode custom = body.get("custom");
        assertNotNull(custom);
        assertEquals("value", custom.asText());
    }

    @Test
    void authorizationEndpointResponseHandlerRejectsInvalidAuthenticationType() {
        AuthorizationEndpointResponseHandler handler = new AuthorizationEndpointResponseHandler();

        assertThrows(
                OAuth2AuthenticationException.class,
                () ->
                        handler.onAuthenticationSuccess(
                                new MockHttpServletRequest(),
                                new MockHttpServletResponse(),
                                new UsernamePasswordAuthenticationToken("user", "pwd")));
    }

    @Test
    void setAccessTokenResponseCustomizerRequiresNonNull() {
        AuthorizationEndpointResponseHandler handler = new AuthorizationEndpointResponseHandler();

        assertThrows(
                IllegalArgumentException.class,
                () -> handler.setAccessTokenResponseCustomizer(null));
    }

    private static OAuth2AccessTokenAuthenticationToken accessTokenAuthentication() {
        RegisteredClient registeredClient =
                RegisteredClient.withId("id")
                        .clientId("client")
                        .clientSecret("secret")
                        .clientAuthenticationMethod(
                                ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(
                                new org.springframework.security.oauth2.core.AuthorizationGrantType("password"))
                        .scope("read")
                        .tokenSettings(TokenSettings.builder().build())
                        .build();
        Authentication clientAuthentication =
                new OAuth2ClientAuthenticationToken(
                        registeredClient,
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                        "secret");
        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token",
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        Set.of("read"));
        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120));
        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientAuthentication, accessToken, refreshToken, Map.of());
    }
}
