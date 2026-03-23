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
 * OAuth2 密码授权类型认证转换器 将使用 `grant_type=password` 的 HTTP 表单请求转换为
 * `OAuth2PasswordAuthorizationGrantAuthenticationToken`，并负责解析与验证参数 主要功能：
 * - 仅处理 grant_type 为 password 的授权请求
 * - 从 Spring Security 上下文获取已认证的客户端信息
 * - 验证 username 和 password 参数的有效性和唯一性
 * - 解析可选的 scope 参数并转换为字符串集合
 * - 收集额外的自定义参数传递给认证令牌 使用示例与 RFC 说明详见源码注释
 *
 * @param request 当前 HTTP 请求，必须为 application/x-www-form-urlencoded 格式的 token 请求
 * @return 如果请求的 grant_type 不是 password 返回 null，否则返回构建好的认证令牌 当请求参数缺失、重复或格式不正确时抛出 grant_type
 *   (REQUIRED) scope (OPTIONAL) // 当请求参数缺失、重复或格式不正确时抛出 // grant_type (REQUIRED) // scope (OPTIONAL)
 * @see Oauth2Properties
 * @see OAuth2PasswordAuthorizationGrantAuthenticationToken
 * @see OAuth2EndpointUtils
 * @author lishangbu
 * @since 2025/9/29 密码授权类型请求参数错误时的 RFC 文档参考链接 指向 RFC 6749 第 4.3.2 节关于密码授权类型的规范说明 OAuth2
 *   配置属性，用于获取自定义的用户名和密码参数名称 将 HTTP 请求转换为 OAuth2 密码授权类型的认证令牌 解析请求体中的表单参数并进行严格验证，确保符合 OAuth2
 *   密码授权类型的规范要求
 */
private const val PASSWORD_REQUEST_ERROR_URI =
    "https://datatracker.ietf.org/doc/html/rfc6749#section-4.3.2"

class OAuth2PasswordAuthenticationConverter(
    private val oauth2Properties: Oauth2Properties,
) : AuthenticationConverter {
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
