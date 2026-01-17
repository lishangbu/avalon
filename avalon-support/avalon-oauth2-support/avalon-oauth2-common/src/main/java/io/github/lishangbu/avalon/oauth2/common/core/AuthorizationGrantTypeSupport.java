package io.github.lishangbu.avalon.oauth2.common.core;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/// 一些过时但仍需支持的授权类型常量
///
/// 提供对密码模式等旧授权类型的兼容支持
///
/// @author lishangbu
@SuppressWarnings({"removal"})
public final class AuthorizationGrantTypeSupport implements Serializable {

  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");
}
