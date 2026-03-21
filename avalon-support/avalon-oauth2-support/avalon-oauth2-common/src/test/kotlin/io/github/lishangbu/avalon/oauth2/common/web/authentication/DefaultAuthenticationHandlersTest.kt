package io.github.lishangbu.avalon.oauth2.common.web.authentication

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.Authentication
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class DefaultAuthenticationHandlersTest {
    private val jsonMapper = JsonMapper()

    @Test
    fun entryPointWritesUnauthorizedJsonResponse() {
        val request = MockHttpServletRequest()
        request.requestURI = "/api"
        val response = MockHttpServletResponse()

        DefaultAuthenticationEntryPoint(jsonMapper)
            .commence(request, response, AuthenticationServiceException("bad credentials"))

        assertEquals(401, response.status)
        assertTrue(response.contentType?.startsWith(MediaType.APPLICATION_JSON_VALUE) == true)
        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals(401, body.get("code").asInt())
        assertTrue(body.get("data").isNull())
        assertEquals("bad credentials", body.get("errorMessage").asText())
    }

    @Test
    fun successHandlerWritesSuccessJsonResponse() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        DefaultAuthenticationSuccessHandler(jsonMapper)
            .onAuthenticationSuccess(request, response, mock(Authentication::class.java))

        assertEquals(200, response.status)
        assertTrue(response.contentType?.startsWith(MediaType.APPLICATION_JSON_VALUE) == true)
        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals(200, body.get("code").asInt())
        assertTrue(body.get("data").isNull())
        assertTrue(body.get("errorMessage").isNull())
    }
}
