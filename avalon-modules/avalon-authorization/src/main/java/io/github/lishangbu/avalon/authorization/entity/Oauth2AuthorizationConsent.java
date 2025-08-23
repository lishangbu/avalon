package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 用户授权确认表(Oauth2AuthorizationConsent)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Oauth2AuthorizationConsent implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 当前授权确认的客户端id */
  private String registeredClientId;

  /** 当前授权确认用户的 username */
  private String principalName;

  /** 授权确认的scope */
  private String authorities;
}
