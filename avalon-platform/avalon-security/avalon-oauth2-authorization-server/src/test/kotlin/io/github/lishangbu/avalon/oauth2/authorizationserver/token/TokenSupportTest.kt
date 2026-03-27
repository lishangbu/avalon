package io.github.lishangbu.avalon.oauth2.authorizationserver.token

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.core.ClaimAccessor
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import java.time.Duration

class TokenSupportTest {
    @Test
    fun refreshTokenGeneratorReturnsNullForOtherTokenTypes() {
        val generator = OAuth2RefreshTokenGenerator()
        val client = registeredClient(OAuth2TokenFormat.REFERENCE)
        val context =
            DefaultOAuth2TokenContext
                .builder()
                .registeredClient(client)
                .principal(UsernamePasswordAuthenticationToken("user", "pwd"))
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build()

        assertNull(generator.generate(context))
    }

    @Test
    fun refreshTokenGeneratorCreatesToken() {
        val generator = OAuth2RefreshTokenGenerator()
        val client = registeredClient(OAuth2TokenFormat.REFERENCE)
        val context =
            DefaultOAuth2TokenContext
                .builder()
                .registeredClient(client)
                .principal(UsernamePasswordAuthenticationToken("user", "pwd"))
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .build()

        val token: OAuth2RefreshToken? = generator.generate(context)

        assertNotNull(token)
        assertTrue(token!!.expiresAt!!.isAfter(token.issuedAt))
    }

    @Test
    fun referenceAccessTokenGeneratorRespectsFormat() {
        val generator = ReferenceOAuth2AccessTokenGenerator()
        val client = registeredClient(OAuth2TokenFormat.SELF_CONTAINED)
        val context =
            DefaultOAuth2TokenContext
                .builder()
                .registeredClient(client)
                .principal(UsernamePasswordAuthenticationToken("user", "pwd"))
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build()

        assertNull(generator.generate(context))
    }

    @Test
    fun referenceAccessTokenGeneratorCreatesTokenWithClaims() {
        val generator = ReferenceOAuth2AccessTokenGenerator()
        val client = registeredClient(OAuth2TokenFormat.REFERENCE)
        val serverContext =
            object : AuthorizationServerContext {
                override fun getIssuer(): String = "https://issuer"

                override fun getAuthorizationServerSettings(): AuthorizationServerSettings = AuthorizationServerSettings.builder().issuer("https://issuer").build()
            }

        val context =
            DefaultOAuth2TokenContext
                .builder()
                .registeredClient(client)
                .authorizationServerContext(serverContext)
                .principal(UsernamePasswordAuthenticationToken("user", "pwd"))
                .authorizedScopes(setOf("read"))
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build()

        val token: OAuth2AccessToken? = generator.generate(context)

        assertNotNull(token)
        assertTrue(token is ClaimAccessor)
        val claims = (token as ClaimAccessor).claims
        assertEquals("https://issuer", claims["iss"].toString())
        assertEquals("user", claims["sub"])
        assertTrue(claims.containsKey(OAuth2ParameterNames.SCOPE))
    }

    @Test
    fun referenceAccessTokenGeneratorOmitsScopeWhenEmpty() {
        val generator = ReferenceOAuth2AccessTokenGenerator()
        val client = registeredClient(OAuth2TokenFormat.REFERENCE)
        val context =
            DefaultOAuth2TokenContext
                .builder()
                .registeredClient(client)
                .principal(UsernamePasswordAuthenticationToken("user", "pwd"))
                .authorizedScopes(emptySet())
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build()

        val token: OAuth2AccessToken? = generator.generate(context)

        assertNotNull(token)
        val claims = (token as ClaimAccessor).claims
        assertFalse(claims.containsKey(OAuth2ParameterNames.SCOPE))
    }

    companion object {
        private fun registeredClient(format: OAuth2TokenFormat): RegisteredClient =
            RegisteredClient
                .withId("id")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .scope("read")
                .tokenSettings(
                    TokenSettings
                        .builder()
                        .accessTokenFormat(format)
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofMinutes(10))
                        .build(),
                ).build()
    }
}
