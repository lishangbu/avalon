package io.github.lishangbu.security.oauth

import io.github.lishangbu.security.rbac.SecurityUserPrincipal
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.ClaimAccessor
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token.CLAIMS_METADATA_NAME
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator

/**
 * Backend 自定义 password grant 的认证提供者。
 *
 * 它复用 Spring Security 用户认证，再把用户权限收敛为客户端和用户都允许的 scope。
 */
class PasswordGrantAuthenticationProvider(
	private val authenticationManager: AuthenticationManager,
	private val authorizationService: OAuth2AuthorizationService,
	private val tokenGenerator: OAuth2TokenGenerator<OAuth2Token>,
) : AuthenticationProvider {
	/**
	 * 校验客户端、用户凭据和 scope 后生成 access token。
	 */
	override fun authenticate(authentication: Authentication): Authentication {
		val passwordGrantAuthentication = authentication as PasswordGrantAuthenticationToken
		val clientPrincipal = passwordGrantAuthentication.clientPrincipal.authenticatedClient()
		val registeredClient = clientPrincipal.registeredClient
			?: throw oauth2Exception(OAuth2ErrorCodes.INVALID_CLIENT)

		if (!registeredClient.authorizationGrantTypes.contains(PASSWORD_GRANT_TYPE)) {
			throw oauth2Exception(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
		}

		val userAuthentication = authenticateUser(passwordGrantAuthentication)
		val principal = userAuthentication.principal as SecurityUserPrincipal
		val authorizedScopes = resolveAuthorizedScopes(
			requestedScopes = passwordGrantAuthentication.scopes,
			clientScopes = registeredClient.scopes,
			userAccessNodes = principal.accessNodes.mapTo(linkedSetOf()) { it.code },
		)

		val tokenContext = DefaultOAuth2TokenContext.builder()
			.registeredClient(registeredClient)
			.principal(userAuthentication)
			.authorizationServerContext(AuthorizationServerContextHolder.getContext())
			.authorizedScopes(authorizedScopes)
			.tokenType(OAuth2TokenType.ACCESS_TOKEN)
			.authorizationGrantType(PASSWORD_GRANT_TYPE)
			.authorizationGrant(passwordGrantAuthentication)
			.build()
		val generatedAccessToken = tokenGenerator.generate(tokenContext)
			?: throw oauth2Exception(OAuth2ErrorCodes.SERVER_ERROR)
		val accessToken = generatedAccessToken.toAccessToken(authorizedScopes)

		val authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
			.principalName(userAuthentication.name)
			.authorizationGrantType(PASSWORD_GRANT_TYPE)
			.authorizedScopes(authorizedScopes)
			.token(accessToken) { metadata ->
				if (generatedAccessToken is ClaimAccessor) {
					metadata[CLAIMS_METADATA_NAME] = generatedAccessToken.claims
				}
			}
			.build()
		authorizationService.save(authorization)

		return OAuth2AccessTokenAuthenticationToken(
			registeredClient,
			clientPrincipal,
			accessToken,
			null,
			emptyMap(),
		)
	}

	override fun supports(authentication: Class<*>): Boolean =
		PasswordGrantAuthenticationToken::class.java.isAssignableFrom(authentication)

	/**
	 * 将用户名密码错误统一映射为 OAuth2 的 invalid_grant，避免泄露账号状态细节。
	 */
	private fun authenticateUser(authentication: PasswordGrantAuthenticationToken): Authentication =
		try {
			authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(authentication.username, authentication.password),
			)
		} catch (_: AuthenticationException) {
			throw oauth2Exception(OAuth2ErrorCodes.INVALID_GRANT)
		}

	/**
	 * 计算实际授权 scope。
	 *
	 * scope 必须同时属于客户端声明和用户权限，防止客户端申请超过用户权限的访问范围。
	 */
	private fun resolveAuthorizedScopes(
		requestedScopes: Set<String>,
		clientScopes: Set<String>,
		userAccessNodes: Set<String>,
	): Set<String> {
		val candidateScopes = if (requestedScopes.isEmpty()) clientScopes else requestedScopes
		if (!clientScopes.containsAll(candidateScopes)) {
			throw oauth2Exception(OAuth2ErrorCodes.INVALID_SCOPE)
		}
		val authorizedScopes = candidateScopes
			.filterTo(linkedSetOf()) { it in userAccessNodes }
		if (authorizedScopes.isEmpty()) {
			throw oauth2Exception(OAuth2ErrorCodes.INVALID_SCOPE)
		}
		return authorizedScopes
	}

	/**
	 * 校验 token 请求已经通过客户端认证。
	 */
	private fun Authentication.authenticatedClient(): OAuth2ClientAuthenticationToken {
		val clientAuthentication = this as? OAuth2ClientAuthenticationToken
		if (clientAuthentication?.isAuthenticated != true) {
			throw oauth2Exception(OAuth2ErrorCodes.INVALID_CLIENT)
		}
		return clientAuthentication
	}

	/**
	 * 将 Spring Authorization Server 生成的 token 统一转换为 Bearer access token。
	 */
	private fun OAuth2Token.toAccessToken(scopes: Set<String>): OAuth2AccessToken =
		when (this) {
			is OAuth2AccessToken -> OAuth2AccessToken(
				tokenType,
				tokenValue,
				issuedAt,
				expiresAt,
				scopes,
			)
			is Jwt -> OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER,
				tokenValue,
				issuedAt,
				expiresAt,
				scopes,
			)
			else -> throw oauth2Exception(OAuth2ErrorCodes.SERVER_ERROR)
		}

	private fun oauth2Exception(errorCode: String): OAuth2AuthenticationException =
		OAuth2AuthenticationException(OAuth2Error(errorCode))
}
