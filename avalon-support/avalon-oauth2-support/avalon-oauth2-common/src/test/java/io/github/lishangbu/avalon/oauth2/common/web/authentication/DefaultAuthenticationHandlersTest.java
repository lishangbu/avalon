package io.github.lishangbu.avalon.oauth2.common.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.github.lishangbu.avalon.json.util.JsonUtils;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DefaultAuthenticationHandlersTest {

    @BeforeAll
    static void initJsonMapper() throws Exception {
        Field field = JsonUtils.class.getDeclaredField("JSON_MAPPER");
        field.setAccessible(true);
        if (field.get(null) == null) {
            field.set(null, new JsonMapper());
        }
    }

    @Test
    void entryPointWritesUnauthorizedJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new DefaultAuthenticationEntryPoint()
                .commence(request, response, new AuthenticationServiceException("bad credentials"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        JsonNode body = JsonUtils.readTree(response.getContentAsString());
        assertEquals(401, body.get("code").asInt());
        assertTrue(body.get("data").isNull());
        assertEquals("bad credentials", body.get("errorMessage").asText());
    }

    @Test
    void successHandlerWritesSuccessJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new DefaultAuthenticationSuccessHandler()
                .onAuthenticationSuccess(request, response, mock(Authentication.class));

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        JsonNode body = JsonUtils.readTree(response.getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertTrue(body.get("data").isNull());
        assertTrue(body.get("errorMessage").isNull());
    }
}
