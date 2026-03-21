package io.github.lishangbu.avalon.oauth2.authorizationserver.introspection

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import java.security.Principal
import java.time.Instant

class DefaultOpaqueTokenIntrospectorTest {
    @Test
    fun throwsWhenAuthorizationMissing() {
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        val introspector = DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService)

        assertThrows(InvalidBearerTokenException::class.java) { introspector.introspect("missing") }
    }

    @Test
    fun returnsPrincipalForClientCredentials() {
        val authorization =
            authorizationWithGrantType(
                AuthorizationGrantType.CLIENT_CREDENTIALS,
                "client",
                mapOf("key" to "val"),
            )
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        Mockito
            .`when`(authorizationService.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
            .thenReturn(authorization)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        val introspector = DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService)

        val principal: OAuth2AuthenticatedPrincipal? = introspector.introspect("token")

        assertNotNull(principal)
        assertEquals("client", principal!!.name)
        assertEquals("val", principal.getAttribute("key"))
        assertEquals(DefaultOAuth2AuthenticatedPrincipal::class.java, principal.javaClass)
    }

    @Test
    fun returnsUserInfoWhenAvailable() {
        val userInfo = UserInfo("user", "pwd", setOf(SimpleGrantedAuthority("ROLE_USER")))
        val principal = UsernamePasswordAuthenticationToken(userInfo, "pwd", userInfo.authorities)
        var authorization =
            authorizationWithGrantType(
                AuthorizationGrantTypeSupport.PASSWORD,
                "user",
                mapOf("scope" to "read"),
            )
        authorization =
            OAuth2Authorization
                .from(authorization)
                .attribute(Principal::class.java.name, principal)
                .build()

        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        Mockito
            .`when`(authorizationService.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
            .thenReturn(authorization)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        Mockito.`when`(userDetailsService.loadUserByUsername("user")).thenReturn(userInfo)

        val introspector = DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService)
        val principalResult = introspector.introspect("token")

        assertSame(userInfo, principalResult)
        assertEquals("read", userInfo.attributes["scope"])
    }

    @Test
    fun returnsNullWhenUserDetailsIsNotUserInfo() {
        val user = User("user", "pwd", setOf(SimpleGrantedAuthority("ROLE_USER")))
        val principal = UsernamePasswordAuthenticationToken(user, "pwd", user.authorities)
        var authorization =
            authorizationWithGrantType(AuthorizationGrantTypeSupport.PASSWORD, "user", emptyMap())
        authorization =
            OAuth2Authorization
                .from(authorization)
                .attribute(Principal::class.java.name, principal)
                .build()

        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        Mockito
            .`when`(authorizationService.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
            .thenReturn(authorization)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        Mockito.`when`(userDetailsService.loadUserByUsername("user")).thenReturn(user)

        val introspector = DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService)
        assertNull(introspector.introspect("token"))
    }

    @Test
    fun rethrowsUsernameNotFound() {
        val user = User("user", "pwd", setOf(SimpleGrantedAuthority("ROLE_USER")))
        val principal = UsernamePasswordAuthenticationToken(user, "pwd", user.authorities)
        var authorization =
            authorizationWithGrantType(AuthorizationGrantTypeSupport.PASSWORD, "user", emptyMap())
        authorization =
            OAuth2Authorization
                .from(authorization)
                .attribute(Principal::class.java.name, principal)
                .build()
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        Mockito
            .`when`(authorizationService.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
            .thenReturn(authorization)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        Mockito
            .`when`(userDetailsService.loadUserByUsername("user"))
            .thenThrow(UsernameNotFoundException("missing"))

        val introspector = DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService)
        assertThrows(UsernameNotFoundException::class.java) { introspector.introspect("token") }
    }

    @Test
    fun returnsNullOnUnexpectedException() {
        val authorization =
            authorizationWithGrantType(AuthorizationGrantTypeSupport.PASSWORD, "user", emptyMap())
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        Mockito
            .`when`(authorizationService.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
            .thenReturn(authorization)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        Mockito
            .`when`(userDetailsService.loadUserByUsername("user"))
            .thenThrow(IllegalStateException("boom"))

        val introspector = DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService)
        assertNull(introspector.introspect("token"))
    }

    companion object {
        private fun authorizationWithGrantType(
            grantType: AuthorizationGrantType,
            principalName: String,
            claims: Map<String, Any>,
        ): OAuth2Authorization {
            val registeredClient =
                RegisteredClient
                    .withId("id")
                    .clientId("client")
                    .clientSecret("secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(grantType)
                    .scope("read")
                    .tokenSettings(TokenSettings.builder().build())
                    .build()
            val accessToken =
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "token",
                    Instant.now(),
                    Instant.now().plusSeconds(60),
                    setOf("read"),
                )
            return OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .principalName(principalName)
                .authorizationGrantType(grantType)
                .token(accessToken) { metadata ->
                    metadata[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = claims
                }.build()
        }
    }
}
