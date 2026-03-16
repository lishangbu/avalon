package io.github.lishangbu.avalon.oauth2.common.core;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/// 一些过时但仍需支持的授权类型常量
///
/// 提供对密码模式等旧授权类型的兼容支持
///
/// @author lishangbu
public final class AuthorizationGrantTypeSupport implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");
    public static final AuthorizationGrantType SMS = new AuthorizationGrantType("sms");
    public static final AuthorizationGrantType EMAIL = new AuthorizationGrantType("email");
}
