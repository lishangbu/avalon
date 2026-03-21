package org.springframework.security.oauth2.server.authorization.web.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthorizationGrantAuthenticationToken

class OAuth2PasswordAuthenticationConverterTest {
    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun returnsNullWhenGrantTypeNotPassword() {
        val converter = OAuth2PasswordAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "sms")

        assertNull(converter.convert(request))
    }

    @Test
    fun throwsWhenUsernameMissingOrDuplicated() {
        val converter = OAuth2PasswordAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.PASSWORD.value,
        )
        request.setParameter("password", "pwd")

        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }

        request.addParameter("username", "user", "another")
        request.setParameter("password", "pwd")
        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }
    }

    @Test
    fun throwsWhenPasswordMissingOrDuplicated() {
        val converter = OAuth2PasswordAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.PASSWORD.value,
        )
        request.setParameter("username", "user")

        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }

        request.addParameter("password", "pwd", "dup")
        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }
    }

    @Test
    fun throwsWhenScopeRepeated() {
        val converter = OAuth2PasswordAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.PASSWORD.value,
        )
        request.setParameter("username", "user")
        request.setParameter("password", "pwd")
        request.addParameter(OAuth2ParameterNames.SCOPE, "read", "write")

        val exception =
            assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }
        assertEquals(OAuth2ErrorCodes.INVALID_REQUEST, exception.error.errorCode)
    }

    @Test
    fun buildsAuthenticationToken() {
        val properties = Oauth2Properties()
        properties.usernameParameterName = "u"
        properties.passwordParameterName = "p"

        val converter = OAuth2PasswordAuthenticationConverter(properties)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("client", "secret")

        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.PASSWORD.value,
        )
        request.setParameter("u", "user")
        request.setParameter("p", "pwd")
        request.setParameter(OAuth2ParameterNames.SCOPE, "read write")
        request.setParameter("custom", "value")
        request.addParameter("multi", "a", "b")

        val token =
            converter.convert(request) as OAuth2PasswordAuthorizationGrantAuthenticationToken

        assertNotNull(token)
        assertEquals("user", token.username)
        assertEquals("pwd", token.password)
        assertEquals(setOf("read", "write"), token.scopes)
        assertEquals("value", token.additionalParameters["custom"])
        assertTrue(token.additionalParameters["multi"] is Array<*>)
    }
}
