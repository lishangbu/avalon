package io.github.lishangbu.avalon.oauth2.authorizationserver.util

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.util.MultiValueMap

class OAuth2EndpointUtilsTest {
    @Test
    fun extractsParametersIntoMultiValueMap() {
        val request = MockHttpServletRequest()
        request.addParameter("scope", "read")
        request.addParameter("scope", "write")
        request.addParameter("grant_type", "password")

        val parameters: MultiValueMap<String, String> = OAuth2EndpointUtils.getParameters(request)

        assertEquals(2, parameters["scope"]?.size)
        assertEquals("password", parameters.getFirst("grant_type"))
    }

    @Test
    fun matchesPkceTokenRequest() {
        val request = MockHttpServletRequest()
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
        request.setParameter(OAuth2ParameterNames.CODE, "code")
        request.setParameter(PkceParameterNames.CODE_VERIFIER, "verifier")

        assertTrue(OAuth2EndpointUtils.matchesPkceTokenRequest(request))

        val missingVerifier = MockHttpServletRequest()
        missingVerifier.setParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
        missingVerifier.setParameter(OAuth2ParameterNames.CODE, "code")
        assertFalse(OAuth2EndpointUtils.matchesPkceTokenRequest(missingVerifier))
    }

    @Test
    fun throwErrorRaisesOAuth2AuthenticationException() {
        val request: HttpServletRequest = MockHttpServletRequest()
        request.parameterMap

        val exception =
            assertThrows(OAuth2AuthenticationException::class.java) {
                OAuth2EndpointUtils.throwError("invalid", "param", "uri")
            }

        assertEquals("invalid", exception.error.errorCode)
        assertEquals("OAuth 2.0 Parameter: param", exception.error.description)
        assertEquals("uri", exception.error.uri)
    }
}
