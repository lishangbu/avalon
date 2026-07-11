package io.github.lishangbu.security.oauth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationConverter

class PublicClientAuthenticationConverter : AuthenticationConverter {
	override fun convert(request: HttpServletRequest): Authentication? {
		if (request.getParameter(OAuth2ParameterNames.GRANT_TYPE) !in supportedGrantTypes) return null
		val clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID)?.takeIf { it.isNotBlank() } ?: return null
		return OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null, emptyMap())
	}

	private companion object {
		val supportedGrantTypes = setOf(PASSWORD_GRANT_TYPE.value, "refresh_token")
	}
}
