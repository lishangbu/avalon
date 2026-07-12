package io.github.lishangbu.security.oauth

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType

/** 即使 access token 是自包含 JWT，也要求其 authorization 尚未被全局撤销。 */
class ActiveAuthorizationJwtValidator(private val authorizations: OAuth2AuthorizationService) : OAuth2TokenValidator<Jwt> {
	override fun validate(token: Jwt): OAuth2TokenValidatorResult =
		if (authorizations.findByToken(token.tokenValue, OAuth2TokenType.ACCESS_TOKEN) != null) {
			OAuth2TokenValidatorResult.success()
		} else {
			OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "Authorization has been revoked", null))
		}
}
