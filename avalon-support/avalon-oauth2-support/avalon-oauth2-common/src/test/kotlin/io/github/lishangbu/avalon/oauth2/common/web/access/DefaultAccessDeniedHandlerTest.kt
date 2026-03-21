package io.github.lishangbu.avalon.oauth2.common.web.access

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class DefaultAccessDeniedHandlerTest {
    private val jsonMapper = JsonMapper()

    @Test
    fun writesForbiddenJsonResponse() {
        val request = MockHttpServletRequest()
        request.requestURI = "/secure"
        val response = MockHttpServletResponse()
        val ex = AccessDeniedException("denied")

        DefaultAccessDeniedHandler(jsonMapper).handle(request, response, ex)

        assertEquals(403, response.status)
        assertTrue(response.contentType?.startsWith(MediaType.APPLICATION_JSON_VALUE) == true)
        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals(403, body.get("code").asInt())
        assertTrue(body.get("data").isNull())
        assertEquals("denied", body.get("errorMessage").asText())
    }
}
