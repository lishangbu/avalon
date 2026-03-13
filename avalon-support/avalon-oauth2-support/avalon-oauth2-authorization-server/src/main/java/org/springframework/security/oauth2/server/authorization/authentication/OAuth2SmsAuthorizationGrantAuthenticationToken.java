package org.springframework.security.oauth2.server.authorization.authentication;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/// OAuth2 短信授权模式的认证令牌
///
/// 用于在 OAuth2 授权服务器中处理短信验证码模式认证请求
/// 封装了手机号、短信验证码、客户端认证信息及附加参数
///
/// @author lishangbu
/// @since 2026/3/13
public class OAuth2SmsAuthorizationGrantAuthenticationToken
        extends OAuth2AuthorizationGrantAuthenticationToken {

    /// 手机号
    @Getter private final String phoneNumber;

    /// 短信验证码
    @Getter private final String smsCode;

    /// 授权范围（scopes）
    @Getter private final Set<String> scopes;

    public OAuth2SmsAuthorizationGrantAuthenticationToken(
            String phoneNumber,
            String smsCode,
            Authentication clientPrincipal,
            @Nullable Set<String> scopes,
            Map<String, Object> additionalParameters) {
        super(AuthorizationGrantTypeSupport.SMS, clientPrincipal, additionalParameters);
        this.phoneNumber = phoneNumber;
        this.smsCode = smsCode;
        this.scopes =
                Collections.unmodifiableSet(
                        (scopes != null) ? new HashSet<>(scopes) : Collections.emptySet());
    }
}
