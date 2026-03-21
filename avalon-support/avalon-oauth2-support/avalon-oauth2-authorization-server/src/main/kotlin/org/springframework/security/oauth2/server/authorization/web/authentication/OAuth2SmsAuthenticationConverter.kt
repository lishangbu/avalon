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
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2SmsAuthorizationGrantAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.util.StringUtils

/**
 * OAuth2 短信授权类型认证转换器 将使用 `grant_type=sms` 的 HTTP 表单请求转换为
 * `OAuth2SmsAuthorizationGrantAuthenticationToken`，并负责解析与验证参数
 *
 * @author lishangbu
 * @since 2026/3/13 短信授权类型请求参数错误时的 RFC 文档参考链接（Extension Grants） grant_type (REQUIRED) scope
 *   (OPTIONAL) // grant_type (REQUIRED) // scope (OPTIONAL)
 */
private const val SMS_REQUEST_ERROR_URI =
    "https://datatracker.ietf.org/doc/html/rfc6749#section-4.5"

class OAuth2SmsAuthenticationConverter(
    private val oauth2Properties: Oauth2Properties,
) : AuthenticationConverter {
    override fun convert(request: HttpServletRequest): Authentication? {
        val parameters = OAuth2EndpointUtils.getFormParameters(request)
        val grantType = parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE)
        if (AuthorizationGrantTypeSupport.SMS.value != grantType) {
            return null
        }

        val clientPrincipal = SecurityContextHolder.getContext().authentication

        val phoneParameterName = oauth2Properties.phoneParameterName
        val phoneNumber = parameters.getFirst(phoneParameterName)
        if (!StringUtils.hasText(phoneNumber) || parameters[phoneParameterName]?.size != 1) {
            OAuth2EndpointUtils.throwError(
                OAuth2ErrorCodes.INVALID_REQUEST,
                phoneParameterName,
                SMS_REQUEST_ERROR_URI,
            )
        }

        val smsCodeParameterName = oauth2Properties.smsCodeParameterName
        val smsCode = parameters.getFirst(smsCodeParameterName)
        if (!StringUtils.hasText(smsCode) || parameters[smsCodeParameterName]?.size != 1) {
            OAuth2EndpointUtils.throwError(
                OAuth2ErrorCodes.INVALID_REQUEST,
                smsCodeParameterName,
                SMS_REQUEST_ERROR_URI,
            )
        }

        val scope = parameters.getFirst(OAuth2ParameterNames.SCOPE)
        if (StringUtils.hasText(scope) && parameters[OAuth2ParameterNames.SCOPE]?.size != 1) {
            OAuth2EndpointUtils.throwError(
                OAuth2ErrorCodes.INVALID_REQUEST,
                OAuth2ParameterNames.SCOPE,
                SMS_REQUEST_ERROR_URI,
            )
        }
        val requestedScopes =
            if (StringUtils.hasText(scope)) {
                LinkedHashSet(StringUtils.delimitedListToStringArray(scope, " ").toList())
            } else {
                null
            }

        val additionalParameters = LinkedHashMap<String, Any>()
        parameters.forEach { key, value ->
            if (
                key != OAuth2ParameterNames.GRANT_TYPE &&
                key != OAuth2ParameterNames.SCOPE &&
                key != OAuth2ParameterNames.CLIENT_ID &&
                key != OAuth2ParameterNames.CLIENT_SECRET &&
                key != phoneParameterName &&
                key != smsCodeParameterName
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

        return OAuth2SmsAuthorizationGrantAuthenticationToken(
            phoneNumber!!,
            smsCode!!,
            authenticatedClient,
            requestedScopes,
            additionalParameters,
        )
    }
}
