package io.github.lishangbu.security.oauth

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

class PublicClientAuthenticationProvider(
	private val clients: RegisteredClientRepository,
) : AuthenticationProvider {
	override fun authenticate(authentication: Authentication): Authentication? {
		val request = authentication as OAuth2ClientAuthenticationToken
		if (request.clientAuthenticationMethod != ClientAuthenticationMethod.NONE) return null
		val client = clients.findByClientId(request.principal.toString())
		if (client == null || ClientAuthenticationMethod.NONE !in client.clientAuthenticationMethods) {
			throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT))
		}
		return OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null)
	}

	override fun supports(authentication: Class<*>): Boolean =
		OAuth2ClientAuthenticationToken::class.java.isAssignableFrom(authentication)
}
