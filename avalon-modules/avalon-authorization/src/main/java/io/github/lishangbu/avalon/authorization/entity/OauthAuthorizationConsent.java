package io.github.lishangbu.avalon.authorization.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/// 用户授权确认表(OauthAuthorizationConsent)实体类
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "用户授权确认表")
@IdClass(OauthAuthorizationConsent.AuthorizationConsentId.class)
public class OauthAuthorizationConsent implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 当前授权确认的客户端id
  @Id
  @Column(comment = "当前授权确认的客户端id", length = 100)
  private String registeredClientId;

  /// 当前授权确认用户的 username
  @Id
  @Column(comment = "当前授权确认用户的 username", length = 200)
  private String principalName;

  /// 授权确认的scope
  @Column(comment = "授权确认的scope", length = 1000)
  private String authorities;

  /// OauthAuthorizationConsent 的复合主键类
  /// 包含 registeredClientId 和 principalName
  @Getter
  @Setter
  public static class AuthorizationConsentId implements Serializable {
    private String registeredClientId;
    private String principalName;

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
}
