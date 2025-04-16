package io.github.lishangbu.avalon.security.properties;

import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT配置
 *
 * @author lishangbu
 * @since 2025/4/8
 */
@ConfigurationProperties(prefix = JwtProperties.JWT_PROPERTIES_PREFIX)
public class JwtProperties {
  public static final String JWT_PROPERTIES_PREFIX =
      SecurityProperties.SECURITY_PROPERTIES_PREFIX + ".jwt";

  /** 公钥路径 */
  private String publicKeyPath = "classpath:rsa/public.key";

  /** 私钥路径 */
  private String privateKeyPath = "classpath:rsa/private.key";

  /** JWT 访问令牌过期时间 */
  private Long accessTokenTtl = 5 * 60L;

  /** JWT访问令牌过期时间单位 */
  private ChronoUnit accessTokenTtlUnit = ChronoUnit.SECONDS;

  /** JWT 访问令牌过期时间 */
  private Long refreshTokenTtl = 30 * 24 * 60 * 60L;

  /** JWT访问令牌过期时间单位 */
  private ChronoUnit refreshTokenTtlUnit = ChronoUnit.SECONDS;

  public String getPublicKeyPath() {
    return publicKeyPath;
  }

  public void setPublicKeyPath(String publicKeyPath) {
    this.publicKeyPath = publicKeyPath;
  }

  public String getPrivateKeyPath() {
    return privateKeyPath;
  }

  public void setPrivateKeyPath(String privateKeyPath) {
    this.privateKeyPath = privateKeyPath;
  }

  public Long getAccessTokenTtl() {
    return accessTokenTtl;
  }

  public void setAccessTokenTtl(Long accessTokenTtl) {
    this.accessTokenTtl = accessTokenTtl;
  }

  public ChronoUnit getAccessTokenTtlUnit() {
    return accessTokenTtlUnit;
  }

  public void setAccessTokenTtlUnit(ChronoUnit accessTokenTtlUnit) {
    this.accessTokenTtlUnit = accessTokenTtlUnit;
  }

  public Long getRefreshTokenTtl() {
    return refreshTokenTtl;
  }

  public void setRefreshTokenTtl(Long refreshTokenTtl) {
    this.refreshTokenTtl = refreshTokenTtl;
  }

  public ChronoUnit getRefreshTokenTtlUnit() {
    return refreshTokenTtlUnit;
  }

  public void setRefreshTokenTtlUnit(ChronoUnit refreshTokenTtlUnit) {
    this.refreshTokenTtlUnit = refreshTokenTtlUnit;
  }
}
