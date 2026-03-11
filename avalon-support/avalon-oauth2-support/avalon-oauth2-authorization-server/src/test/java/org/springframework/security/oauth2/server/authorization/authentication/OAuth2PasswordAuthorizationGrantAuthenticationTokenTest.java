package org.springframework.security.oauth2.server.authorization.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class OAuth2PasswordAuthorizationGrantAuthenticationTokenTest {

    @Test
    void defaultsToEmptyScopes() {
        OAuth2PasswordAuthorizationGrantAuthenticationToken token =
                new OAuth2PasswordAuthorizationGrantAuthenticationToken(
                        "user",
                        "pwd",
                        new UsernamePasswordAuthenticationToken("client", "secret"),
                        null,
                        Map.of());

        assertTrue(token.getScopes().isEmpty());
        assertEquals("user", token.getUsername());
        assertEquals("pwd", token.getPassword());
        assertEquals(AuthorizationGrantTypeSupport.PASSWORD, token.getGrantType());
    }

    @Test
    void scopesAreUnmodifiableCopies() {
        Set<String> scopes = Set.of("read", "write");
        OAuth2PasswordAuthorizationGrantAuthenticationToken token =
                new OAuth2PasswordAuthorizationGrantAuthenticationToken(
                        "user",
                        "pwd",
                        new UsernamePasswordAuthenticationToken("client", "secret"),
                        scopes,
                        Map.of());

        assertEquals(2, token.getScopes().size());
        assertThrows(UnsupportedOperationException.class, () -> token.getScopes().add("extra"));
    }
}
