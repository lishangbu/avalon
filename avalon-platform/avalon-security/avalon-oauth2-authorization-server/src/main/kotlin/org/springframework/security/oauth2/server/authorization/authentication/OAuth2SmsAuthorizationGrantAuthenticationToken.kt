package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.springframework.security.core.Authentication

/**
 * OAuth2 短信授权认证令牌
 *
 * 封装短信验证码授权请求中的手机号、验证码和附加参数
 */
class OAuth2SmsAuthorizationGrantAuthenticationToken(
    /** 手机号 */
    val phoneNumber: String,
    /** 短信验证码 */
    val smsCode: String,
    clientPrincipal: Authentication,
    scopes: Set<String>?,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(
        AuthorizationGrantTypeSupport.SMS,
        clientPrincipal,
        additionalParameters,
    ) {
    /** 授权范围 */
    val scopes: Set<String> = java.util.Set.copyOf(scopes.orEmpty())
}
