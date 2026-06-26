package io.github.lishangbu.security.oauth

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken

/**
 * Backend password grant 的授权类型值。
 */
const val PASSWORD_GRANT_TYPE_VALUE: String = "urn:security:params:oauth:grant-type:password"

/**
 * Backend password grant 的授权类型。
 */
val PASSWORD_GRANT_TYPE: AuthorizationGrantType =
	AuthorizationGrantType(PASSWORD_GRANT_TYPE_VALUE)

/**
 * token endpoint 解析后的 password grant 认证请求。
 */
class PasswordGrantAuthenticationToken(
	val username: String,
	val password: String,
	val clientPrincipal: Authentication,
	val scopes: Set<String>,
	additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(
	PASSWORD_GRANT_TYPE,
	clientPrincipal,
	additionalParameters,
)
