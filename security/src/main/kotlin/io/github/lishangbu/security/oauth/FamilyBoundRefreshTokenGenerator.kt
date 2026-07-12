package io.github.lishangbu.security.oauth

import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Duration
import java.time.Instant

const val REFRESH_TOKEN_FAMILY_STARTED_AT = "avalon.refresh-token-family-started-at"
private val REFRESH_TOKEN_FAMILY_MAX_AGE: Duration = Duration.ofDays(7)

/** 对标准旋转 refresh token 施加 family 首次登录起七天的绝对上限。 */
class FamilyBoundRefreshTokenGenerator(
	private val delegate: OAuth2TokenGenerator<OAuth2RefreshToken> = OAuth2RefreshTokenGenerator(),
) : OAuth2TokenGenerator<OAuth2RefreshToken> {
	override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
		val token = delegate.generate(context) ?: return null
		val rawStartedAt = context.authorization?.getAttribute<Any>(REFRESH_TOKEN_FAMILY_STARTED_AT) ?: return token
		val epochSecond = when (rawStartedAt) {
			is Number -> rawStartedAt.toLong()
			is String -> rawStartedAt.toLongOrNull()
			else -> null
		} ?: return token
		return boundRefreshToken(token, Instant.ofEpochSecond(epochSecond))
	}
}

/** 保留令牌随机值与签发时间，只收紧过期时间，避免滑动续期突破 family deadline。 */
internal fun boundRefreshToken(token: OAuth2RefreshToken, familyStartedAt: Instant): OAuth2RefreshToken {
	val deadline = familyStartedAt.plus(REFRESH_TOKEN_FAMILY_MAX_AGE)
	val expiresAt = token.expiresAt?.let { minOf(it, deadline) } ?: deadline
	return OAuth2RefreshToken(token.tokenValue, token.issuedAt, expiresAt)
}
