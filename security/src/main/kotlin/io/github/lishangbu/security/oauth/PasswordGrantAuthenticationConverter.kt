package io.github.lishangbu.security.oauth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

/**
 * 将 token endpoint 的 password grant 请求转换为 Backend 认证令牌。
 */
class PasswordGrantAuthenticationConverter(
	private val registeredClientRepository: RegisteredClientRepository? = null,
) : AuthenticationConverter {
	/**
	 * 只处理 Backend 自定义 grant type，其它 grant type 交给授权服务器默认 converter。
	 */
	override fun convert(request: HttpServletRequest): Authentication? {
		val grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE)
		if (grantType != PASSWORD_GRANT_TYPE.value) {
			return null
		}

		val parameters = request.toParameters()
		val username = parameters.singleRequired("username")
		val password = parameters.singleRequired("password")
		val scopes = parameters.singleOptional(OAuth2ParameterNames.SCOPE)
			?.split(" ")
			.orEmpty()
			.filter { it.isNotBlank() }
			.toSet()
		val clientPrincipal = SecurityContextHolder.getContext().authentication
			?: parameters.publicClientPrincipal()
			?: throw invalidRequest()
		val additionalParameters = parameters
			.filterKeys { it !in reservedParameters }
			.mapValues { it.value.first() }

		return PasswordGrantAuthenticationToken(
			username = username,
			password = password,
			clientPrincipal = clientPrincipal,
			scopes = scopes,
			additionalParameters = additionalParameters,
		)
	}

	/**
	 * 保留重复参数，后续单值校验会把重复敏感参数判为 invalid_request。
	 */
	private fun HttpServletRequest.toParameters(): MultiValueMap<String, String> {
		val result = LinkedMultiValueMap<String, String>()
		parameterMap.forEach { (name, values) ->
			values.forEach { value -> result.add(name, value) }
		}
		return result
	}

	/**
	 * 读取必填单值参数。
	 */
	private fun MultiValueMap<String, String>.singleRequired(name: String): String {
		val value = singleOptional(name)
		if (value.isNullOrBlank()) {
			throw invalidRequest()
		}
		return value
	}

	/**
	 * 读取可选单值参数，重复提交视为非法请求。
	 */
	private fun MultiValueMap<String, String>.singleOptional(name: String): String? {
		val values = this[name].orEmpty()
		if (values.size > 1) {
			throw invalidRequest()
		}
		return values.firstOrNull()
	}

	private fun MultiValueMap<String, String>.publicClientPrincipal(): Authentication? {
		val clientId = singleOptional(OAuth2ParameterNames.CLIENT_ID) ?: return null
		val client = registeredClientRepository?.findByClientId(clientId) ?: return null
		if (!client.clientAuthenticationMethods.contains(ClientAuthenticationMethod.NONE)) return null
		return OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null)
	}

	private fun invalidRequest(): OAuth2AuthenticationException =
		OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST))

	private companion object {
		private val reservedParameters = setOf(
			OAuth2ParameterNames.GRANT_TYPE,
			OAuth2ParameterNames.SCOPE,
			"username",
			"password",
			OAuth2ParameterNames.CLIENT_ID,
		)
	}
}
