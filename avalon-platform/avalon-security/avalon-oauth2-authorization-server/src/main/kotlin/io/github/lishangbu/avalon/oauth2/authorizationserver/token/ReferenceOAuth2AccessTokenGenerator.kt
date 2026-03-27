package io.github.lishangbu.avalon.oauth2.authorizationserver.token

import io.github.lishangbu.avalon.oauth2.authorizationserver.keygen.UuidKeyGenerator
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.oauth2.core.ClaimAccessor
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsSet
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Instant
import java.util.*

/**
 * 引用型访问令牌生成器
 *
 * 生成携带声明集的 reference access token
 */
class ReferenceOAuth2AccessTokenGenerator : OAuth2TokenGenerator<OAuth2AccessToken> {
    /** 访问令牌生成器 */
    private val accessTokenGenerator: StringKeyGenerator = UuidKeyGenerator()

    /** 生成引用型访问令牌 */
    override fun generate(context: OAuth2TokenContext): OAuth2AccessToken? {
        if (
            OAuth2TokenType.ACCESS_TOKEN != context.tokenType ||
            OAuth2TokenFormat.REFERENCE !=
            context.registeredClient.tokenSettings.accessTokenFormat
        ) {
            return null
        }

        val issuer = context.authorizationServerContext?.issuer
        val registeredClient: RegisteredClient = context.registeredClient
        val authorizedScopes = context.authorizedScopes ?: emptySet()

        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(registeredClient.tokenSettings.accessTokenTimeToLive)

        val claimsBuilder = OAuth2TokenClaimsSet.builder()
        issuer?.takeIf(String::isNotBlank)?.let(claimsBuilder::issuer)
        claimsBuilder
            .subject(context.getPrincipal<Authentication>().name)
            .audience(listOf(registeredClient.clientId))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .notBefore(issuedAt)
            .id(UUID.randomUUID().toString())
        if (authorizedScopes.isNotEmpty()) {
            claimsBuilder.claim(OAuth2ParameterNames.SCOPE, authorizedScopes)
        }
        val accessTokenClaimsSet = claimsBuilder.build()

        return OAuth2AccessTokenClaims(
            OAuth2AccessToken.TokenType.BEARER,
            accessTokenGenerator.generateKey(),
            accessTokenClaimsSet.issuedAt,
            accessTokenClaimsSet.expiresAt,
            LinkedHashSet(authorizedScopes),
            LinkedHashMap(accessTokenClaimsSet.claims),
        )
    }

    private class OAuth2AccessTokenClaims(
        tokenType: OAuth2AccessToken.TokenType,
        tokenValue: String,
        issuedAt: Instant?,
        expiresAt: Instant?,
        scopes: MutableSet<String>,
        /** 声明集 */
        private val claims: MutableMap<String, Any>,
    ) : OAuth2AccessToken(tokenType, tokenValue, issuedAt, expiresAt, scopes),
        ClaimAccessor {
        /** 获取声明 */
        override fun getClaims(): MutableMap<String, Any> = claims
    }
}
