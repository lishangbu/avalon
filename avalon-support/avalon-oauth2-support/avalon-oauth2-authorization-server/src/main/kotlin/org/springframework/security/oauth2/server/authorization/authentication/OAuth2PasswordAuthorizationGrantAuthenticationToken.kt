package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.springframework.security.core.Authentication
import java.util.*

/**
 * OAuth2 密码授权模式的认证令牌 用于在 OAuth2 授权服务器中处理密码模式（Resource Owner Password Credentials Grant）认证请求
 * 封装了用户名、密码、客户端认证信息及附加参数
 *
 * @param username 资源拥有者的用户名
 * @param password 资源拥有者的密码
 * @param clientPrincipal 已认证的客户端信息
 * @param scopes 授权范围
 * @param additionalParameters 附加参数
 * @see OAuth2AuthorizationCodeAuthenticationToken
 * @see OAuth2RefreshTokenAuthenticationToken
 * @see OAuth2ClientCredentialsAuthenticationToken
 * @author xuxiaowei
 * @author lishangbu
 * @since 2025/9/28 资源拥有者的用户名 资源拥有者的密码 授权范围（scopes） 子类构造方法 用于创建 OAuth2 密码授权模式的认证令牌实例
 */
class OAuth2PasswordAuthorizationGrantAuthenticationToken(
    val username: String,
    val password: String,
    clientPrincipal: Authentication,
    scopes: Set<String>?,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(
        AuthorizationGrantTypeSupport.PASSWORD,
        clientPrincipal,
        additionalParameters,
    ) {
    val scopes: Set<String> =
        Collections.unmodifiableSet(if (scopes != null) HashSet(scopes) else emptySet())
}
