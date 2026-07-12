package io.github.lishangbu.security.oauth

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 把 refresh token 的锁定读取、校验、生成与持久化包在同一个数据库事务中。 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
open class RefreshTokenRotationService(
	authorizations: OAuth2AuthorizationService,
	tokenGenerator: OAuth2TokenGenerator<OAuth2Token>,
) {
	private val delegate = OAuth2RefreshTokenAuthenticationProvider(authorizations, tokenGenerator)

	@Transactional
	open fun authenticate(authentication: Authentication): Authentication = delegate.authenticate(authentication)
}
