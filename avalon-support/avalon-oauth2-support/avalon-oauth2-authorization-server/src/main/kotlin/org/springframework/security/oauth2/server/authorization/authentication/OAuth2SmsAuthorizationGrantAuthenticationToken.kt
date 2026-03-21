package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.springframework.security.core.Authentication
import java.util.*

/**
 * OAuth2 短信授权模式的认证令牌 用于在 OAuth2 授权服务器中处理短信验证码模式认证请求 封装了手机号、短信验证码、客户端认证信息及附加参数
 *
 * @author lishangbu
 * @since 2026/3/13 手机号 短信验证码 授权范围（scopes）
 */
class OAuth2SmsAuthorizationGrantAuthenticationToken(
    val phoneNumber: String,
    val smsCode: String,
    clientPrincipal: Authentication,
    scopes: Set<String>?,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(
        AuthorizationGrantTypeSupport.SMS,
        clientPrincipal,
        additionalParameters,
    ) {
    val scopes: Set<String> =
        Collections.unmodifiableSet(if (scopes != null) HashSet(scopes) else emptySet())
}
