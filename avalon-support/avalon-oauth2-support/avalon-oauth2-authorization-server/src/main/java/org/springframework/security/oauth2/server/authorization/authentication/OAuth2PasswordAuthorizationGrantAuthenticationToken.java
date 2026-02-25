package org.springframework.security.oauth2.server.authorization.authentication;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/// OAuth2 密码授权模式的认证令牌
///
/// 用于在 OAuth2 授权服务器中处理密码模式（Resource Owner Password Credentials Grant）认证请求
/// 封装了用户名、密码、客户端认证信息及附加参数
///
/// @author xuxiaowei
/// @author lishangbu
/// @see OAuth2AuthorizationCodeAuthenticationToken
/// @see OAuth2RefreshTokenAuthenticationToken
/// @see OAuth2ClientCredentialsAuthenticationToken
/// @since 2025/9/28
public class OAuth2PasswordAuthorizationGrantAuthenticationToken
        extends OAuth2AuthorizationGrantAuthenticationToken {

    /// 资源拥有者的用户名
    @Getter private final String username;

    /// 资源拥有者的密码
    @Getter private final String password;

    /// 授权范围（scopes）
    @Getter private final Set<String> scopes;

    /// 子类构造方法
    ///
    /// 用于创建 OAuth2 密码授权模式的认证令牌实例
    ///
    /// @param username             资源拥有者的用户名
    /// @param password             资源拥有者的密码
    /// @param clientPrincipal      已认证的客户端信息
    /// @param scopes               授权范围
    /// @param additionalParameters 附加参数
    public OAuth2PasswordAuthorizationGrantAuthenticationToken(
            String username,
            String password,
            Authentication clientPrincipal,
            @Nullable Set<String> scopes,
            Map<String, Object> additionalParameters) {
        super(AuthorizationGrantTypeSupport.PASSWORD, clientPrincipal, additionalParameters);
        this.username = username;
        this.password = password;
        this.scopes =
                Collections.unmodifiableSet(
                        (scopes != null) ? new HashSet<>(scopes) : Collections.emptySet());
    }
}
