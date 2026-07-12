package io.github.lishangbu.security.oauth

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken

/** 在一个数据库事务中锁定、消费并旋转 refresh token，使并发重放只能有一个请求成功。 */
class SerializedRefreshTokenAuthenticationProvider(
	private val rotations: RefreshTokenRotationService,
	private val replayRevocations: RefreshTokenReplayRevocationService,
) : AuthenticationProvider {
	override fun authenticate(authentication: Authentication): Authentication = try {
		rotations.authenticate(authentication)
	} catch (replay: RefreshTokenReplayDetectedException) {
		// rotation 事务已经回滚并释放行锁，此时独立提交 family 删除，不会被 invalid_grant 回滚。
		replayRevocations.revoke(replay.authorizationId)
		throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT))
	}

	override fun supports(authentication: Class<*>): Boolean =
		OAuth2RefreshTokenAuthenticationToken::class.java.isAssignableFrom(authentication)
}
