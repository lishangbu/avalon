package io.github.lishangbu.security.oauth

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken

/**
 * reference access token 的资源服务器认证提供者。
 */
class OpaqueTokenAuthenticationProvider(
	private val authorizationService: org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService,
) : AuthenticationProvider {
	/**
	 * 从授权服务读取 token 状态并恢复 Backend 权限。
	 */
	override fun authenticate(authentication: Authentication): Authentication {
		val tokenValue = (authentication as BearerTokenAuthenticationToken).token
		val authorization = authorizationService.findByToken(tokenValue, OAuth2TokenType.ACCESS_TOKEN)
			?: throw InvalidBearerTokenException("Invalid access token")
		val accessToken = authorization.accessToken
			?: throw InvalidBearerTokenException("Invalid access token")

		if (!accessToken.isActive) {
			throw InvalidBearerTokenException("Inactive access token")
		}

		val claims = accessToken.claims.orEmpty()
		val authorities = securityAuthoritiesFromClaims(claims)
		val principal = DefaultOAuth2AuthenticatedPrincipal(
			claims.subjectOrPrincipalName(authorization),
			claims,
			authorities,
		)
		return BearerTokenAuthentication(principal, accessToken.token, authorities)
	}

	override fun supports(authentication: Class<*>): Boolean =
		BearerTokenAuthenticationToken::class.java.isAssignableFrom(authentication)

	/**
	 * 优先使用 token subject，缺失时回退到授权记录中的 principalName。
	 */
	private fun Map<String, Any>.subjectOrPrincipalName(authorization: OAuth2Authorization): String =
		this["sub"]?.toString() ?: authorization.principalName
}
