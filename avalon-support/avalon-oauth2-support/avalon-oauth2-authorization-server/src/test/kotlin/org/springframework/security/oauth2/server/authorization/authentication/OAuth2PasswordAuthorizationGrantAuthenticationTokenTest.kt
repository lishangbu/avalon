package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

class OAuth2PasswordAuthorizationGrantAuthenticationTokenTest {
    @Test
    fun defaultsToEmptyScopes() {
        val token =
            OAuth2PasswordAuthorizationGrantAuthenticationToken(
                "user",
                "pwd",
                UsernamePasswordAuthenticationToken("client", "secret"),
                null,
                mapOf(),
            )

        assertTrue(token.scopes.isEmpty())
        assertEquals("user", token.username)
        assertEquals("pwd", token.password)
        assertEquals(AuthorizationGrantTypeSupport.PASSWORD, token.grantType)
    }

    @Test
    fun scopesAreUnmodifiableCopies() {
        val scopes = setOf("read", "write")
        val token =
            OAuth2PasswordAuthorizationGrantAuthenticationToken(
                "user",
                "pwd",
                UsernamePasswordAuthenticationToken("client", "secret"),
                scopes,
                mapOf(),
            )

        assertEquals(2, token.scopes.size)
        assertThrows(UnsupportedOperationException::class.java) {
            (token.scopes as MutableSet<String>).add("extra")
        }
    }
}
