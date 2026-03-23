package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.springframework.security.core.Authentication

/**
 * OAuth2 密码授权认证令牌
 *
 * 封装密码授权请求中的用户名、密码和附加参数
 */
class OAuth2PasswordAuthorizationGrantAuthenticationToken(
    /** 用户名 */
    val username: String,
    /** 密码 */
    val password: String,
    clientPrincipal: Authentication,
    scopes: Set<String>?,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(
        AuthorizationGrantTypeSupport.PASSWORD,
        clientPrincipal,
        additionalParameters,
    ) {
    /** 授权范围 */
    val scopes: Set<String> = java.util.Set.copyOf(scopes.orEmpty())
}
