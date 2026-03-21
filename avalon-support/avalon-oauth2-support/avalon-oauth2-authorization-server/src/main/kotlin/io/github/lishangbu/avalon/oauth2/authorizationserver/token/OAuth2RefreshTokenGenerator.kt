package io.github.lishangbu.avalon.oauth2.authorizationserver.token

import io.github.lishangbu.avalon.oauth2.authorizationserver.keygen.UuidKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Instant

/**
 * 自定义 RefreshToken 生成器 无论何种授权方式，均返回一个基于 UUID 的 refresh token
 *
 * @author lishangbu
 * @since 2025/8/22 @formatter:off @formatter:on // @formatter:off // @formatter:on
 */
class OAuth2RefreshTokenGenerator : OAuth2TokenGenerator<OAuth2RefreshToken> {
    private val refreshTokenGenerator: StringKeyGenerator = UuidKeyGenerator()

    override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
        if (OAuth2TokenType.REFRESH_TOKEN != context.tokenType) {
            return null
        }
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(context.registeredClient.tokenSettings.refreshTokenTimeToLive)
        return OAuth2RefreshToken(refreshTokenGenerator.generateKey(), issuedAt, expiresAt)
    }
}
