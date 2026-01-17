package io.github.lishangbu.avalon.authorization.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
public class OauthRegisteredClient implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 唯一标识符
  private String id;

  /// 客户端 ID
  private String clientId;

  /// 客户端 ID 签发时间
  private Instant clientIdIssuedAt;

  /// 客户端密钥
  private String clientSecret;

  /// 客户端密钥过期时间
  private Instant clientSecretExpiresAt;

  /// 客户端名称
  private String clientName;

  /// 客户端认证方式
  private String clientAuthenticationMethods;

  /// 授权方式
  private String authorizationGrantTypes;

  /// 重定向 URI
  private String redirectUris;

  /// 登出后的重定向 URI
  private String postLogoutRedirectUris;

  /// 客户端授权的范围
  private String scopes;

  /// true if the client is required to provide a proof key challenge and verifier when performing
  /// the Authorization Code Grant flow. The default is false.
  private Boolean requireProofKey;

  /// true if authorization consent is required when the client requests access. The default is
  // false
  private Boolean requireAuthorizationConsent;

  /// the URL for the Client's JSON Web Key Set
  private String jwkSetUrl;

  /// the JWS algorithm that must be used for signing the JWT used to authenticate the Client at the
  /// Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods.
  private String tokenEndpointAuthenticationSigningAlgorithm;

  /// the expected subject distinguished name associated to the client X509Certificate received
  /// during client authentication when using the tls_client_auth method
  @TableField("x509_certificate_subject_dn")
  private String x509CertificateSubjectDn;

  /// the time-to-live for an authorization code. The default is 5 minutes.
  private String authorizationCodeTimeToLive;

  /// the time-to-live for an access token. The default is 5 minutes.
  private String accessTokenTimeToLive;

  /// the token format for an access token,The default is self-contained
  private String accessTokenFormat;

  /// the time-to-live for an access token. The default is 5 minutes.
  private String deviceCodeTimeToLive;

  /// Returns true if refresh tokens are reused when returning the access token response, or false
  // if
  /// a new refresh token is issued. The default is true.
  private Boolean reuseRefreshTokens;

  /// the time-to-live for a refresh token. The default is 60 minutes.
  private String refreshTokenTimeToLive;

  /// the JWS algorithm for signing the ID Token. The default is RS256.
  private String idTokenSignatureAlgorithm;

  /// true if access tokens must be bound to the client X509Certificate received during client
  /// authentication when using the tls_client_auth or self_signed_tls_client_auth method. The
  /// default is false.
  @TableField("x509_certificate_bound_access_tokens")
  private Boolean x509CertificateBoundAccessTokens;
}
