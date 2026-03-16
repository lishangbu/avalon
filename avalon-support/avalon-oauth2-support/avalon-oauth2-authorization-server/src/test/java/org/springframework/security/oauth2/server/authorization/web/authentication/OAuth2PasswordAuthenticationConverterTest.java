package org.springframework.security.oauth2.server.authorization.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthorizationGrantAuthenticationToken;

class OAuth2PasswordAuthenticationConverterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsNullWhenGrantTypeNotPassword() {
        OAuth2PasswordAuthenticationConverter converter =
                new OAuth2PasswordAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "client_credentials");

        assertNull(converter.convert(request));
    }

    @Test
    void throwsWhenUsernameMissingOrDuplicated() {
        OAuth2PasswordAuthenticationConverter converter =
                new OAuth2PasswordAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.PASSWORD.getValue());
        request.setParameter("password", "pwd");

        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));

        request.addParameter("username", "u1", "u2");
        request.setParameter("password", "pwd");
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }

    @Test
    void throwsWhenPasswordMissingOrDuplicated() {
        OAuth2PasswordAuthenticationConverter converter =
                new OAuth2PasswordAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.PASSWORD.getValue());
        request.setParameter("username", "user");

        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));

        request.addParameter("password", "p1", "p2");
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }

    @Test
    void throwsWhenScopeRepeated() {
        OAuth2PasswordAuthenticationConverter converter =
                new OAuth2PasswordAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.PASSWORD.getValue());
        request.setParameter("username", "user");
        request.setParameter("password", "pwd");
        request.addParameter(OAuth2ParameterNames.SCOPE, "read", "write");

        OAuth2AuthenticationException exception =
                assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
        assertEquals(OAuth2ErrorCodes.INVALID_REQUEST, exception.getError().getErrorCode());
    }

    @Test
    void buildsAuthenticationToken() {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setUsernameParameterName("user");
        properties.setPasswordParameterName("pwd");

        OAuth2PasswordAuthenticationConverter converter =
                new OAuth2PasswordAuthenticationConverter(properties);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("client", "secret"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.PASSWORD.getValue());
        request.setParameter("user", "alice");
        request.setParameter("pwd", "pass");
        request.setParameter(OAuth2ParameterNames.SCOPE, "read write");
        request.setParameter("custom", "value");
        request.addParameter("multi", "a", "b");

        OAuth2PasswordAuthorizationGrantAuthenticationToken token =
                (OAuth2PasswordAuthorizationGrantAuthenticationToken) converter.convert(request);

        assertNotNull(token);
        assertEquals("alice", token.getUsername());
        assertEquals("pass", token.getPassword());
        assertEquals(Set.of("read", "write"), token.getScopes());
        assertEquals("value", token.getAdditionalParameters().get("custom"));
        assertTrue(token.getAdditionalParameters().get("multi") instanceof String[]);
    }
}
