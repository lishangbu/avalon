package io.github.lishangbu.security.oauth

import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken

/**
 * 验证自定义密码授权请求只在匹配 grant type 时转换，并拒绝歧义参数。
 */
class PasswordGrantAuthenticationConverterTests {
	@Test
	fun `converts backend password grant request`() {
		val clientPrincipal = OAuth2ClientAuthenticationToken(
			"system-admin-jwt",
			ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
			null,
			emptyMap(),
		)
		SecurityContextHolder.getContext().authentication = clientPrincipal
		val request = MockHttpServletRequest().form(
			OAuth2ParameterNames.GRANT_TYPE to PASSWORD_GRANT_TYPE.value,
			"username" to "admin",
			"password" to "secret",
			OAuth2ParameterNames.SCOPE to "security:admin",
		)

		val token = PasswordGrantAuthenticationConverter().convert(request)

		assertThat(token).isInstanceOf(PasswordGrantAuthenticationToken::class.java)
		val passwordToken = token as PasswordGrantAuthenticationToken
		assertThat(passwordToken.username).isEqualTo("admin")
		assertThat(passwordToken.password).isEqualTo("secret")
		assertThat(passwordToken.clientPrincipal).isSameAs(clientPrincipal)
		assertThat(passwordToken.scopes).containsExactly("security:admin")
	}

	@Test
	fun `ignores other grant types`() {
		val request = MockHttpServletRequest().form(
			OAuth2ParameterNames.GRANT_TYPE to "client_credentials",
		)

		assertThat(PasswordGrantAuthenticationConverter().convert(request)).isNull()
	}

	@Test
	fun `rejects duplicated username parameter`() {
		val request = MockHttpServletRequest().apply {
			method = "POST"
			addParameter(OAuth2ParameterNames.GRANT_TYPE, PASSWORD_GRANT_TYPE.value)
			addParameter("username", "admin", "other")
			addParameter("password", "secret")
		}

		org.junit.jupiter.api.assertThrows<OAuth2AuthenticationException> {
			PasswordGrantAuthenticationConverter().convert(request)
		}
	}

	private fun MockHttpServletRequest.form(vararg pairs: Pair<String, String>): HttpServletRequest {
		method = "POST"
		pairs.forEach { addParameter(it.first, it.second) }
		return this
	}
}
