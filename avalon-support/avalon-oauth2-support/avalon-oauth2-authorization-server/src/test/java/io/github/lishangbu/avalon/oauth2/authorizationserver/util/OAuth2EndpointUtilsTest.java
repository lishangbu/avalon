package io.github.lishangbu.avalon.oauth2.authorizationserver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.util.MultiValueMap;

class OAuth2EndpointUtilsTest {

    @Test
    void extractsParametersIntoMultiValueMap() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("scope", "read");
        request.addParameter("scope", "write");
        request.addParameter("grant_type", "password");

        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        assertEquals(2, parameters.get("scope").size());
        assertEquals("password", parameters.getFirst("grant_type"));
    }

    @Test
    void matchesPkceTokenRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        request.setParameter(OAuth2ParameterNames.CODE, "code");
        request.setParameter(PkceParameterNames.CODE_VERIFIER, "verifier");

        assertTrue(OAuth2EndpointUtils.matchesPkceTokenRequest(request));

        MockHttpServletRequest missingVerifier = new MockHttpServletRequest();
        missingVerifier.setParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        missingVerifier.setParameter(OAuth2ParameterNames.CODE, "code");
        assertFalse(OAuth2EndpointUtils.matchesPkceTokenRequest(missingVerifier));
    }

    @Test
    void throwErrorRaisesOAuth2AuthenticationException() {
        HttpServletRequest request = new MockHttpServletRequest();

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> OAuth2EndpointUtils.throwError("invalid", "param", "uri"));

        assertEquals("invalid", exception.getError().getErrorCode());
        assertEquals("OAuth 2.0 Parameter: param", exception.getError().getDescription());
        assertEquals("uri", exception.getError().getUri());
    }
}
