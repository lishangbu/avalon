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
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2EmailAuthorizationGrantAuthenticationToken

class OAuth2EmailAuthenticationConverterTest {
    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun returnsNullWhenGrantTypeNotEmail() {
        val converter = OAuth2EmailAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "password")

        assertNull(converter.convert(request))
    }

    @Test
    fun throwsWhenEmailMissingOrDuplicated() {
        val converter = OAuth2EmailAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.EMAIL.value,
        )
        request.setParameter("email_code", "123456")

        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }

        request.addParameter("email", "user@example.com", "another@example.com")
        request.setParameter("email_code", "123456")
        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }
    }

    @Test
    fun throwsWhenEmailCodeMissingOrDuplicated() {
        val converter = OAuth2EmailAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.EMAIL.value,
        )
        request.setParameter("email", "user@example.com")

        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }

        request.addParameter("email_code", "123456", "654321")
        assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }
    }

    @Test
    fun throwsWhenScopeRepeated() {
        val converter = OAuth2EmailAuthenticationConverter(Oauth2Properties())
        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.EMAIL.value,
        )
        request.setParameter("email", "user@example.com")
        request.setParameter("email_code", "123456")
        request.addParameter(OAuth2ParameterNames.SCOPE, "read", "write")

        val exception =
            assertThrows(OAuth2AuthenticationException::class.java) { converter.convert(request) }
        assertEquals(OAuth2ErrorCodes.INVALID_REQUEST, exception.error.errorCode)
    }

    @Test
    fun buildsAuthenticationToken() {
        val properties = Oauth2Properties()
        properties.emailParameterName = "mail"
        properties.emailCodeParameterName = "code"

        val converter = OAuth2EmailAuthenticationConverter(properties)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("client", "secret")

        val request = MockHttpServletRequest()
        request.setParameter(
            OAuth2ParameterNames.GRANT_TYPE,
            AuthorizationGrantTypeSupport.EMAIL.value,
        )
        request.setParameter("mail", "user@example.com")
        request.setParameter("code", "123456")
        request.setParameter(OAuth2ParameterNames.SCOPE, "read write")
        request.setParameter("custom", "value")
        request.addParameter("multi", "a", "b")

        val token = converter.convert(request) as OAuth2EmailAuthorizationGrantAuthenticationToken

        assertNotNull(token)
        assertEquals("user@example.com", token.email)
        assertEquals("123456", token.emailCode)
        assertEquals(setOf("read", "write"), token.scopes)
        assertEquals("value", token.additionalParameters["custom"])
        assertTrue(token.additionalParameters["multi"] is Array<*>)
    }
}
