package io.github.lishangbu.avalon.authorization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/// Oauth2 注册客户端 (OauthRegisteredClient) 实体类
///
/// 表示注册在系统中的 OAuth2 客户端及其配置，用于映射数据库中的注册客户端表
///
/// @author lishangbu
/// @since 2025/08/19
@Data
@Entity
@Table(comment = "OAuth2 注册客户端")
public class OauthRegisteredClient implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 唯一标识符
  @Id
  @Column(comment = "唯一标识符", length = 100)
  private String id;

  /// 客户端 ID
  @Column(comment = "客户端 ID", length = 100)
  private String clientId;

  /// 客户端 ID 签发时间
  @Column(comment = "客户端 ID 签发时间")
  private Instant clientIdIssuedAt;

  /// 客户端密钥
  @Column(comment = "客户端密钥", length = 200)
  private String clientSecret;

  /// 客户端密钥过期时间
  @Column(comment = "客户端密钥过期时间")
  private Instant clientSecretExpiresAt;

  /// 客户端名称
  @Column(comment = "客户端名称", length = 200)
  private String clientName;

  /// 客户端认证方式
  @Column(comment = "客户端认证方式", length = 1000)
  private String clientAuthenticationMethods;

  /// 授权方式
  @Column(comment = "授权方式", length = 1000)
  private String authorizationGrantTypes;

  /// 重定向 URI
  @Column(comment = "重定向 URI", length = 1000)
  private String redirectUris;

  /// 登出后的重定向 URI
  @Column(comment = "登出后的重定向 URI", length = 1000)
  private String postLogoutRedirectUris;

  /// 客户端授权的范围
  @Column(comment = "客户端授权的范围", length = 1000)
  private String scopes;

  /// 如果客户端在执行授权码授权流程时需要提供证明密钥挑战和验证器，则为 true。默认值为 false。
  @Column(comment = "如果客户端在执行授权码授权流程时需要提供证明密钥挑战和验证器，则为 true。默认值为 false。")
  private Boolean requireProofKey;

  /// 如果客户端请求访问时需要授权同意，则为 true。默认值为 false。
  @Column(comment = "如果客户端请求访问时需要授权同意，则为 true。默认值为 false。")
  private Boolean requireAuthorizationConsent;

  /// 客户端的 JSON Web Key Set 的 URL
  @Column(comment = "客户端的 JSON Web Key Set 的 URL", length = 1000)
  private String jwkSetUrl;

  /// 用于在令牌端点对客户端进行身份验证的 JWT 签名时必须使用的 JWS 算法（private_key_jwt 和 client_secret_jwt 认证方法）。
  @Column(
      comment = "用于在令牌端点对客户端进行身份验证的 JWT 签名时必须使用的 JWS 算法（private_key_jwt 和 client_secret_jwt 认证方法）。",
      length = 20)
  private String tokenEndpointAuthenticationSigningAlgorithm;

  /// 使用 tls_client_auth 方法时，在客户端认证期间接收到的客户端 X509Certificate 关联的预期主题可分辨名称。
  @Column(
      name = "x509_certificate_subject_dn",
      comment = "使用 tls_client_auth 方法时，在客户端认证期间接收到的客户端 X509Certificate 关联的预期主题可分辨名称。",
      length = 20)
  private String x509CertificateSubjectDn;

  /// 授权码的生存时间。默认值为 5 分钟。
  @Column(comment = "授权码的生存时间。默认值为 5 分钟。", length = 20)
  private String authorizationCodeTimeToLive;

  /// 访问令牌的生存时间。默认值为 5 分钟。
  @Column(comment = "访问令牌的生存时间。默认值为 5 分钟。", length = 20)
  private String accessTokenTimeToLive;

  /// 访问令牌的令牌格式，默认值为 self-contained。
  @Column(comment = "访问令牌的令牌格式，默认值为 self-contained。", length = 20)
  private String accessTokenFormat;

  /// 设备码的生存时间。默认值为 5 分钟。
  @Column(comment = "设备码的生存时间。默认值为 5 分钟。", length = 20)
  private String deviceCodeTimeToLive;

  /// 如果在返回访问令牌响应时重用刷新令牌，则为 true；如果发出新的刷新令牌，则为 false。默认值为 true。
  @Column(comment = "如果在返回访问令牌响应时重用刷新令牌，则为 true；如果发出新的刷新令牌，则为 false。默认值为 true。")
  private Boolean reuseRefreshTokens;

  /// 刷新令牌的生存时间。默认值为 60 分钟。
  @Column(comment = "刷新令牌的生存时间。默认值为 60 分钟。", length = 20)
  private String refreshTokenTimeToLive;

  /// 用于签名 ID 令牌的 JWS 算法。默认值为 RS256。
  @Column(comment = "用于签名 ID 令牌的 JWS 算法。默认值为 RS256。", length = 20)
  private String idTokenSignatureAlgorithm;

  /// 如果访问令牌必须绑定到使用 tls_client_auth 或 self_signed_tls_client_auth 方法时在客户端认证期间接收到的客户端
  // X509Certificate，则为 true。默认值为 false。
  @Column(
      name = "x509_certificate_bound_access_tokens",
      comment =
          "如果访问令牌必须绑定到使用 tls_client_auth 或 self_signed_tls_client_auth 方法时在客户端认证期间接收到的客户端"
              + " X509Certificate，则为 true。默认值为 false。")
  private Boolean x509CertificateBoundAccessTokens;
}
