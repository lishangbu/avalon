package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

/**
 * 用户授权确认表(OauthAuthorizationConsent)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class OauthAuthorizationConsent implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 复合主键 - registered_client_id */
  private String registeredClientId;

  /** 复合主键 - principal_name */
  private String principalName;

  /** 授权确认的scope */
  private String authorities;

  @Override
  public final int hashCode() {
    return Objects.hash(registeredClientId, principalName);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof OauthAuthorizationConsent that)) return false;
    return Objects.equals(registeredClientId, that.registeredClientId)
        && Objects.equals(principalName, that.principalName);
  }
}
