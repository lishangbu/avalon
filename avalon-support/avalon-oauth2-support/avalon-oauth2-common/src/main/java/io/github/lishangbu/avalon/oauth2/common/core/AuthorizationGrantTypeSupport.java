package io.github.lishangbu.avalon.oauth2.common.core;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * 一些过时但还需要使用的授权类型
 *
 * @author lishangbu
 */
@SuppressWarnings({"removal"})
public final class AuthorizationGrantTypeSupport implements Serializable {

  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");
}
