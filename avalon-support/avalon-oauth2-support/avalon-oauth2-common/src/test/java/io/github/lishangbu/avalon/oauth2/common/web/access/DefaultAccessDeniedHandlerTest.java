package io.github.lishangbu.avalon.oauth2.common.web.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.json.util.JsonUtils;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DefaultAccessDeniedHandlerTest {

    @BeforeAll
    static void initJsonMapper() throws Exception {
        Field field = JsonUtils.class.getDeclaredField("JSON_MAPPER");
        field.setAccessible(true);
        if (field.get(null) == null) {
            field.set(null, new JsonMapper());
        }
    }

    @Test
    void writesForbiddenJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/secure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException ex = new AccessDeniedException("denied");

        new DefaultAccessDeniedHandler().handle(request, response, ex);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        JsonNode body = JsonUtils.readTree(response.getContentAsString());
        assertEquals(403, body.get("code").asInt());
        assertTrue(body.get("data").isNull());
        assertEquals("denied", body.get("errorMessage").asText());
    }
}
