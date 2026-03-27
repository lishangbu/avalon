package org.springframework.security.oauth2.server.authorization.web.authentication

import io.github.lishangbu.avalon.oauth2.authorizationserver.util.OAuth2EndpointUtils
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthorizationGrantAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationConverter

/**
 * OAuth2 密码授权请求转换器
 *
 * 将密码授权请求转换为认证令牌
 */
private const val PASSWORD_REQUEST_ERROR_URI =
    "https://datatracker.ietf.org/doc/html/rfc6749#section-4.3.2"

class OAuth2PasswordAuthenticationConverter(
    /** OAuth2 属性 */
    private val oauth2Properties: Oauth2Properties,
) : AuthenticationConverter {
    /** 将请求转换为认证令牌 */
    override fun convert(request: HttpServletRequest): Authentication? {
        val parameters = OAuth2EndpointUtils.getFormParameters(request)
        val grantType = parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE)
        if (AuthorizationGrantTypeSupport.PASSWORD.value != grantType) {
            return null
        }

        val clientPrincipal = SecurityContextHolder.getContext().authentication

        val usernameParameterName = oauth2Properties.usernameParameterName
        val username =
            parameters.requireSingleTextParameter(usernameParameterName, PASSWORD_REQUEST_ERROR_URI)

        val passwordParameterName = oauth2Properties.passwordParameterName
        val password =
            parameters.requireSingleTextParameter(passwordParameterName, PASSWORD_REQUEST_ERROR_URI)
        val requestedScopes = parameters.readRequestedScopes(PASSWORD_REQUEST_ERROR_URI)
        val additionalParameters = LinkedHashMap<String, Any>()
        parameters.forEach { key, value ->
            if (
                key != OAuth2ParameterNames.GRANT_TYPE &&
                key != OAuth2ParameterNames.SCOPE &&
                key != OAuth2ParameterNames.CLIENT_ID &&
                key != OAuth2ParameterNames.CLIENT_SECRET &&
                key != usernameParameterName &&
                key != passwordParameterName
            ) {
                additionalParameters[key] =
                    if (value.size == 1) {
                        value.first()
                    } else {
                        value.toTypedArray()
                    }
            }
        }

        val authenticatedClient =
            clientPrincipal ?: throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)

        return OAuth2PasswordAuthorizationGrantAuthenticationToken(
            username,
            password,
            authenticatedClient,
            requestedScopes,
            additionalParameters,
        )
    }
}
