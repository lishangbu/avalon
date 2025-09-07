package io.github.lishangbu.avalon.authorization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 用户认证信息表(OauthAuthorization)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class OauthAuthorization implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 唯一标识符 */
  @Id private String id;

  /** 已注册的客户端 ID */
  private String registeredClientId;

  /** 主体名称 */
  private String principalName;

  /** 授权方式 */
  private String authorizationGrantType;

  /** 授权范围 */
  private String authorizedScopes;

  /** 属性 */
  private String attributes;

  /** 状态 */
  private String state;

  /** 授权码值 */
  private String authorizationCodeValue;

  /** 授权码签发时间 */
  private Instant authorizationCodeIssuedAt;

  /** 授权码过期时间 */
  private Instant authorizationCodeExpiresAt;

  /** 授权码元数据 */
  private String authorizationCodeMetadata;

  /** 访问令牌值 */
  private String accessTokenValue;

  /** 访问令牌签发时间 */
  private Instant accessTokenIssuedAt;

  /** 访问令牌过期时间 */
  private Instant accessTokenExpiresAt;

  /** 访问令牌元数据 */
  private String accessTokenMetadata;

  /** 访问令牌范围 */
  private String accessTokenScopes;

  /** OIDC ID 令牌值 */
  private String oidcIdTokenValue;

  /** OIDC ID 令牌签发时间 */
  private Instant oidcIdTokenIssuedAt;

  /** OIDC ID 令牌过期时间 */
  private Instant oidcIdTokenExpiresAt;

  /** OIDC ID 令牌元数据 */
  private String oidcIdTokenMetadata;

  /** OIDC ID 令牌声明 */
  private String oidcIdTokenClaims;

  /** 刷新令牌值 */
  private String refreshTokenValue;

  /** 刷新令牌签发时间 */
  private Instant refreshTokenIssuedAt;

  /** 刷新令牌过期时间 */
  private Instant refreshTokenExpiresAt;

  /** 刷新令牌元数据 */
  private String refreshTokenMetadata;

  /** 用户代码值 */
  private String userCodeValue;

  /** 用户代码签发时间 */
  private Instant userCodeIssuedAt;

  /** 用户代码过期时间 */
  private Instant userCodeExpiresAt;

  /** 用户代码元数据 */
  private String userCodeMetadata;

  /** 设备代码值 */
  private String deviceCodeValue;

  /** 设备代码签发时间 */
  private Instant deviceCodeIssuedAt;

  /** 设备代码过期时间 */
  private Instant deviceCodeExpiresAt;

  /** 设备代码元数据 */
  private String deviceCodeMetadata;

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
    OauthAuthorization that = (OauthAuthorization) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
