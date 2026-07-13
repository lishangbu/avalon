package io.github.lishangbu.security.oauth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken

/**
 * 根据 bearer token 形态选择 JWT 或 reference token 认证管理器。
 */
class BearerTokenAuthenticationManagerResolver(
	jwtAuthenticationProvider: AuthenticationProvider,
	opaqueAuthenticationProvider: AuthenticationProvider,
) : AuthenticationManagerResolver<HttpServletRequest> {
	private val jwtAuthenticationManager = AuthenticationManager {
		jwtAuthenticationProvider.authenticate(it) ?: throw BadCredentialsException("Invalid bearer token")
	}
	private val opaqueAuthenticationManager = AuthenticationManager {
		opaqueAuthenticationProvider.authenticate(it) ?: throw BadCredentialsException("Invalid bearer token")
	}

	/**
	 * JWT 使用两个点分隔三段；其它 token 交给 reference token 提供者校验。
	 */
	override fun resolve(context: HttpServletRequest): AuthenticationManager =
		AuthenticationManager { authentication ->
			authenticate((authentication as BearerTokenAuthenticationToken).token)
		}

	/** 浏览器 WebSocket 首帧无法复用 Servlet bearer filter，因此复用同一 token 形态分流与认证链。 */
	fun authenticate(token: String) = BearerTokenAuthenticationToken(token).let { authentication ->
		if (token.count { it == '.' } == 2) {
			jwtAuthenticationManager.authenticate(authentication)
		} else {
			opaqueAuthenticationManager.authenticate(authentication)
		}
	}
}
