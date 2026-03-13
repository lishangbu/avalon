package org.springframework.security.oauth2.server.authorization.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class OAuth2SmsAuthorizationGrantAuthenticationTokenTest {

    @Test
    void defaultsToEmptyScopes() {
        OAuth2SmsAuthorizationGrantAuthenticationToken token =
                new OAuth2SmsAuthorizationGrantAuthenticationToken(
                        "13800000000",
                        "123456",
                        new UsernamePasswordAuthenticationToken("client", "secret"),
                        null,
                        Map.of());

        assertTrue(token.getScopes().isEmpty());
        assertEquals("13800000000", token.getPhoneNumber());
        assertEquals("123456", token.getSmsCode());
        assertEquals(AuthorizationGrantTypeSupport.SMS, token.getGrantType());
    }

    @Test
    void scopesAreUnmodifiableCopies() {
        Set<String> scopes = Set.of("read", "write");
        OAuth2SmsAuthorizationGrantAuthenticationToken token =
                new OAuth2SmsAuthorizationGrantAuthenticationToken(
                        "13800000000",
                        "123456",
                        new UsernamePasswordAuthenticationToken("client", "secret"),
                        scopes,
                        Map.of());

        assertEquals(2, token.getScopes().size());
        assertThrows(UnsupportedOperationException.class, () -> token.getScopes().add("extra"));
    }
}
