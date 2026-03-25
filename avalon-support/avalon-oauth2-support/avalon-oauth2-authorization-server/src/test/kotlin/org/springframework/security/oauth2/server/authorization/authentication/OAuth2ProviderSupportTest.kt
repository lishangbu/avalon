package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import java.time.Duration
import java.time.Instant

class OAuth2ProviderSupportTest {
    @Test
    fun buildLockMessageUsesSecondsForShortLocks() {
        assertEquals("账号已被锁定，请在1秒后重试", buildLockMessage(null))
        assertEquals("账号已被锁定，请在59秒后重试", buildLockMessage(Duration.ofSeconds(59)))
    }

    @Test
    fun buildLockMessageRoundsUpToMinutes() {
        assertEquals("账号已被锁定，请在1分钟后重试", buildLockMessage(Duration.ofSeconds(60)))
        assertEquals("账号已被锁定，请在2分钟后重试", buildLockMessage(Duration.ofSeconds(61)))
    }

    @Test
    fun buildAccessTokenUsesDpopAndPersistsMetadataForClaimTokens() {
        val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
        val expiresAt = issuedAt.plusSeconds(300)
        val authorizationBuilder =
            OAuth2Authorization
                .withRegisteredClient(registeredClient())
                .principalName("user")
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
        val generatedToken =
            Jwt
                .withTokenValue("access-token")
                .header("alg", "RS256")
                .claim("sub", "user")
                .claim("cnf", mapOf("jkt" to "thumbprint"))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build()

        val accessToken = buildAccessToken(authorizationBuilder, generatedToken, tokenContext())
        val authorization = authorizationBuilder.build()
        val token = authorization.getToken(OAuth2AccessToken::class.java)!!

        assertEquals(OAuth2AccessToken.TokenType.DPOP, accessToken.tokenType)
        assertEquals(setOf("read", OidcScopes.OPENID), accessToken.scopes)
        assertEquals(false, token.metadata[OAuth2Authorization.Token.INVALIDATED_METADATA_NAME])
        assertEquals(OAuth2TokenFormat.REFERENCE.value, token.metadata[OAuth2TokenFormat::class.java.name])
        assertEquals("user", requireNotNull(token.claims)["sub"])
        assertTrue(requireNotNull(token.claims).containsKey("cnf"))
    }

    @Test
    fun buildAccessTokenKeepsBearerForPlainTokens() {
        val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
        val expiresAt = issuedAt.plusSeconds(300)
        val authorizationBuilder =
            OAuth2Authorization
                .withRegisteredClient(registeredClient())
                .principalName("user")
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
        val generatedToken =
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "plain-access-token",
                issuedAt,
                expiresAt,
                setOf("read"),
            )

        val accessToken = buildAccessToken(authorizationBuilder, generatedToken, tokenContext())
        val authorization = authorizationBuilder.build()
        val token = authorization.getToken(OAuth2AccessToken::class.java)!!

        assertEquals(OAuth2AccessToken.TokenType.BEARER, accessToken.tokenType)
        assertEquals(null, token.claims)
    }

    companion object {
        private fun tokenContext() =
            DefaultOAuth2TokenContext
                .builder()
                .registeredClient(registeredClient())
                .principal(UsernamePasswordAuthenticationToken("user", "password"))
                .authorizedScopes(setOf("read", OidcScopes.OPENID))
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build()

        private fun registeredClient(): RegisteredClient =
            RegisteredClient
                .withId("id")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .scope("read")
                .scope(OidcScopes.OPENID)
                .tokenSettings(
                    TokenSettings
                        .builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofMinutes(10))
                        .build(),
                ).build()
    }
}
