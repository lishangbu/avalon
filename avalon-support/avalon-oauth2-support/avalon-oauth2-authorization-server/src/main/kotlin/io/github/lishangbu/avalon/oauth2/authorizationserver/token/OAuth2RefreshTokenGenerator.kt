package io.github.lishangbu.avalon.oauth2.authorizationserver.token

import io.github.lishangbu.avalon.oauth2.authorizationserver.keygen.UuidKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Instant

/**
 * 刷新令牌生成器
 *
 * 基于 UUID 生成 refresh token
 */
class OAuth2RefreshTokenGenerator : OAuth2TokenGenerator<OAuth2RefreshToken> {
    /** 刷新令牌生成器 */
    private val refreshTokenGenerator: StringKeyGenerator = UuidKeyGenerator()

    /** 生成刷新令牌 */
    override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
        if (OAuth2TokenType.REFRESH_TOKEN != context.tokenType) {
            return null
        }
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(context.registeredClient.tokenSettings.refreshTokenTimeToLive)
        return OAuth2RefreshToken(refreshTokenGenerator.generateKey(), issuedAt, expiresAt)
    }
}
