package io.github.lishangbu.avalon.authorization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 用户授权确认表(OauthAuthorizationConsent)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@IdClass(OauthAuthorizationConsent.AuthorizationConsentId.class)
public class OauthAuthorizationConsent implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 当前授权确认的客户端id */
  @Id private String registeredClientId;

  /** 当前授权确认用户的 username */
  @Id private String principalName;

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
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass =
        o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    OauthAuthorizationConsent that = (OauthAuthorizationConsent) o;
    return getRegisteredClientId() != null
        && Objects.equals(getRegisteredClientId(), that.getRegisteredClientId())
        && getPrincipalName() != null
        && Objects.equals(getPrincipalName(), that.getPrincipalName());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(registeredClientId, principalName);
  }
}
