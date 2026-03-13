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
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2EmailAuthorizationGrantAuthenticationToken;

class OAuth2EmailAuthenticationConverterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsNullWhenGrantTypeNotEmail() {
        OAuth2EmailAuthenticationConverter converter =
                new OAuth2EmailAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "password");

        assertNull(converter.convert(request));
    }

    @Test
    void throwsWhenEmailMissingOrDuplicated() {
        OAuth2EmailAuthenticationConverter converter =
                new OAuth2EmailAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.EMAIL.getValue());
        request.setParameter("email_code", "123456");

        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));

        request.addParameter("email", "user@example.com", "another@example.com");
        request.setParameter("email_code", "123456");
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }

    @Test
    void throwsWhenEmailCodeMissingOrDuplicated() {
        OAuth2EmailAuthenticationConverter converter =
                new OAuth2EmailAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.EMAIL.getValue());
        request.setParameter("email", "user@example.com");

        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));

        request.addParameter("email_code", "123456", "654321");
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }

    @Test
    void throwsWhenScopeRepeated() {
        OAuth2EmailAuthenticationConverter converter =
                new OAuth2EmailAuthenticationConverter(new Oauth2Properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.EMAIL.getValue());
        request.setParameter("email", "user@example.com");
        request.setParameter("email_code", "123456");
        request.addParameter(OAuth2ParameterNames.SCOPE, "read", "write");

        OAuth2AuthenticationException exception =
                assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
        assertEquals(OAuth2ErrorCodes.INVALID_REQUEST, exception.getError().getErrorCode());
    }

    @Test
    void buildsAuthenticationToken() {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setEmailParameterName("mail");
        properties.setEmailCodeParameterName("code");

        OAuth2EmailAuthenticationConverter converter = new OAuth2EmailAuthenticationConverter(properties);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("client", "secret"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantTypeSupport.EMAIL.getValue());
        request.setParameter("mail", "user@example.com");
        request.setParameter("code", "123456");
        request.setParameter(OAuth2ParameterNames.SCOPE, "read write");
        request.setParameter("custom", "value");
        request.addParameter("multi", "a", "b");

        OAuth2EmailAuthorizationGrantAuthenticationToken token =
                (OAuth2EmailAuthorizationGrantAuthenticationToken) converter.convert(request);

        assertNotNull(token);
        assertEquals("user@example.com", token.getEmail());
        assertEquals("123456", token.getEmailCode());
        assertEquals(Set.of("read", "write"), token.getScopes());
        assertEquals("value", token.getAdditionalParameters().get("custom"));
        assertTrue(token.getAdditionalParameters().get("multi") instanceof String[]);
    }
}
