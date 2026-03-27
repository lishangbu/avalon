package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.springframework.security.core.Authentication

/**
 * OAuth2 邮箱授权认证令牌
 *
 * 封装邮箱验证码授权请求中的邮箱、验证码和附加参数
 */
class OAuth2EmailAuthorizationGrantAuthenticationToken(
    /** 邮箱 */
    val email: String,
    /** 邮箱验证码 */
    val emailCode: String,
    clientPrincipal: Authentication,
    scopes: Set<String>?,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(
        AuthorizationGrantTypeSupport.EMAIL,
        clientPrincipal,
        additionalParameters,
    ) {
    /** 授权范围 */
    val scopes: Set<String> = java.util.Set.copyOf(scopes.orEmpty())
}
