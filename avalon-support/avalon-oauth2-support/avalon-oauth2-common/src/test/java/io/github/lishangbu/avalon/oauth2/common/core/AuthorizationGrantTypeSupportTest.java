package io.github.lishangbu.avalon.oauth2.common.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class AuthorizationGrantTypeSupportTest {

    @Test
    void passwordGrantTypeMatchesStandardValue() {
        AuthorizationGrantType expected = new AuthorizationGrantType("password");

        assertEquals(expected, AuthorizationGrantTypeSupport.PASSWORD);
        assertEquals("password", AuthorizationGrantTypeSupport.PASSWORD.getValue());
    }
}
