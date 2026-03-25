package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import java.time.Instant

class TokenControllerTest {
    private val authorizationService = mock(OAuth2AuthorizationService::class.java)
    private val controller = TokenController(authorizationService)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun logoutRemovesAuthorizationAndClearsContext() {
        val accessToken =
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(60),
            )
        val authentication = mock(Authentication::class.java)
        val authorization = mock(OAuth2Authorization::class.java)
        `when`(authentication.credentials).thenReturn(accessToken)
        `when`(
            authorizationService.findByToken("access-token", OAuth2TokenType.ACCESS_TOKEN),
        ).thenReturn(authorization)
        SecurityContextHolder.getContext().authentication = authentication

        controller.logout()

        verify(authorizationService).remove(authorization)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun logoutIgnoresNonOauthAccessTokens() {
        val authentication = mock(Authentication::class.java)
        `when`(authentication.credentials).thenReturn("plain-text")
        SecurityContextHolder.getContext().authentication = authentication

        controller.logout()

        assertSame(authentication, SecurityContextHolder.getContext().authentication)
        verifyNoInteractions(authorizationService)
    }

    @Test
    fun userReturnsCurrentPrincipal() {
        val principal = UserInfo("alice", "{noop}pwd", AuthorityUtils.NO_AUTHORITIES)

        assertSame(principal, controller.user(principal))
    }
}
