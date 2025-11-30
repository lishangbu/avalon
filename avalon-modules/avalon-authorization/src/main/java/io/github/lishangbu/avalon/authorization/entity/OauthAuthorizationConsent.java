package io.github.lishangbu.avalon.authorization.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 用户授权确认表(OauthAuthorizationConsent)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class OauthAuthorizationConsent implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  @Embedded.Nullable
  private AuthorizationConsentId id;

  /** 当前授权确认的客户端id */
  private String registeredClientId;

  /** 当前授权确认用户的 username */
  private String principalName;

  /** 授权确认的scope */
  private String authorities;

  public static class AuthorizationConsentId implements Serializable {
    private String registeredClientId;
    private String principalName;

    public String getRegisteredClientId() {
      return registeredClientId;
    }

    public void setRegisteredClientId(String registeredClientId) {
      this.registeredClientId = registeredClientId;
    }

    public String getPrincipalName() {
      return principalName;
    }

    public void setPrincipalName(String principalName) {
      this.principalName = principalName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      AuthorizationConsentId that = (AuthorizationConsentId) o;
      return registeredClientId.equals(that.registeredClientId)
          && principalName.equals(that.principalName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(registeredClientId, principalName);
    }
  }

  @Override
  public final int hashCode() {
    return Objects.hash(registeredClientId, principalName);
  }
}
