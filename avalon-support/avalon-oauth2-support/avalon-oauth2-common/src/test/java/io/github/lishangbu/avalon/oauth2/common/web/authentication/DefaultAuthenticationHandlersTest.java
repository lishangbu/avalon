package io.github.lishangbu.avalon.oauth2.common.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DefaultAuthenticationHandlersTest {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    @Test
    void entryPointWritesUnauthorizedJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new DefaultAuthenticationEntryPoint(JSON_MAPPER)
                .commence(request, response, new AuthenticationServiceException("bad credentials"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        JsonNode body = JSON_MAPPER.readTree(response.getContentAsString());
        assertEquals(401, body.get("code").asInt());
        assertTrue(body.get("data").isNull());
        assertEquals("bad credentials", body.get("errorMessage").asText());
    }

    @Test
    void successHandlerWritesSuccessJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new DefaultAuthenticationSuccessHandler(JSON_MAPPER)
                .onAuthenticationSuccess(request, response, mock(Authentication.class));

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        JsonNode body = JSON_MAPPER.readTree(response.getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertTrue(body.get("data").isNull());
        assertTrue(body.get("errorMessage").isNull());
    }
}
