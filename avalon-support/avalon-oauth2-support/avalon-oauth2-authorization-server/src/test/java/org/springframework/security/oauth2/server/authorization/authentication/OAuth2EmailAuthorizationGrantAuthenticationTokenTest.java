package org.springframework.security.oauth2.server.authorization.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class OAuth2EmailAuthorizationGrantAuthenticationTokenTest {

    @Test
    void defaultsToEmptyScopes() {
        OAuth2EmailAuthorizationGrantAuthenticationToken token =
                new OAuth2EmailAuthorizationGrantAuthenticationToken(
                        "user@example.com",
                        "123456",
                        new UsernamePasswordAuthenticationToken("client", "secret"),
                        null,
                        Map.of());

        assertTrue(token.getScopes().isEmpty());
        assertEquals("user@example.com", token.getEmail());
        assertEquals("123456", token.getEmailCode());
        assertEquals(AuthorizationGrantTypeSupport.EMAIL, token.getGrantType());
    }

    @Test
    void scopesAreUnmodifiableCopies() {
        Set<String> scopes = Set.of("read", "write");
        OAuth2EmailAuthorizationGrantAuthenticationToken token =
                new OAuth2EmailAuthorizationGrantAuthenticationToken(
                        "user@example.com",
                        "123456",
                        new UsernamePasswordAuthenticationToken("client", "secret"),
                        scopes,
                        Map.of());

        assertEquals(2, token.getScopes().size());
        assertThrows(UnsupportedOperationException.class, () -> token.getScopes().add("extra"));
    }
}
