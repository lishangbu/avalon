package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import lombok.Data;

/// 用户认证信息表 (OauthAuthorization) 实体类
///
/// 存储 OAuth2 授权相关的持久化信息，包括授权码、访问令牌、刷新令牌、OIDC ID 令牌及其元数据
///
/// @author lishangbu
/// @since 2025/08/20
@Data
public class OauthAuthorization implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 授权记录唯一标识符，数据库主键，最大长度 100 个字符
  private String id;

  /// 注册客户端 ID，关联到 OAuth2 客户端，标识发起授权请求的客户端应用，最大长度 100 个字符
  private String registeredClientId;

  /// 用户主体名称，标识被授权的用户，通常为用户名或用户唯一标识，最大长度 200 个字符
  private String principalName;

  /// 授权类型，标识使用的授权流程，可选值：authorization_code、client_credentials、refresh_token、device_code 等，最大长度 100
  // 个字符
  private String authorizationGrantType;

  /// 已授权的权限范围集合，逗号分隔的权限字符串，记录用户实际授予客户端的权限，最大长度 1000 个字符
  private String authorizedScopes;

  /// 授权属性数据，存储授权相关的元数据
  private Map<String, Object> attributes;

  /// 授权状态信息，存储授权流程的状态数据，最大长度 500 个字符
  private String state;

  /// 授权码值，授权码模式下生成的临时授权码
  private String authorizationCodeValue;

  /// 授权码签发时间（UTC）
  private Instant authorizationCodeIssuedAt;

  /// 授权码过期时间（UTC）
  private Instant authorizationCodeExpiresAt;

  /// 授权码元数据，存储授权码相关的附加信息
  private Map<String, Object> authorizationCodeMetadata;

  /// 访问令牌值，用于访问受保护资源的凭证
  private String accessTokenValue;

  /// 访问令牌签发时间（UTC）
  private Instant accessTokenIssuedAt;

  /// 访问令牌过期时间（UTC）
  private Instant accessTokenExpiresAt;

  /// 访问令牌元数据，存储访问令牌相关的附加信息
  private Map<String, Object> accessTokenMetadata;

  /// 访问令牌类型，标识令牌的使用方式（通常为 Bearer），最大长度 100 个字符
  private String accessTokenType;

  /// 访问令牌关联的权限范围，逗号分隔的权限字符串，最大长度 1000 个字符
  private String accessTokenScopes;

  /// OpenID Connect ID 令牌值，包含用户身份信息的 JWT 令牌
  private String oidcIdTokenValue;

  /// ID 令牌签发时间（UTC）
  private Instant oidcIdTokenIssuedAt;

  /// ID 令牌过期时间（UTC）
  private Instant oidcIdTokenExpiresAt;

  /// ID 令牌元数据，存储 ID 令牌相关的附加信息
  private Map<String, Object> oidcIdTokenMetadata;

  /// 刷新令牌值，用于获取新访问令牌的长期凭证
  private String refreshTokenValue;

  /// 刷新令牌签发时间（UTC）
  private Instant refreshTokenIssuedAt;

  /// 刷新令牌过期时间（UTC）
  private Instant refreshTokenExpiresAt;

  /// 刷新令牌元数据，存储刷新令牌相关的附加信息
  private Map<String, Object> refreshTokenMetadata;

  /// 用户码值，设备授权流程中用户输入的验证码
  private String userCodeValue;

  /// 用户码签发时间（UTC）
  private Instant userCodeIssuedAt;

  /// 用户码过期时间（UTC）
  private Instant userCodeExpiresAt;

  /// 用户码元数据，存储用户码相关的附加信息
  private Map<String, Object> userCodeMetadata;

  /// 设备码值，设备授权流程中设备使用的验证码
  private String deviceCodeValue;

  /// 设备码签发时间（UTC）
  private Instant deviceCodeIssuedAt;

  /// 设备码过期时间（UTC）
  private Instant deviceCodeExpiresAt;

  /// 设备码元数据，存储设备码相关的附加信息
  private Map<String, Object> deviceCodeMetadata;
}
