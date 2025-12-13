package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import lombok.Data;

/**
 * 用户认证信息表(OauthAuthorization)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class OauthAuthorization implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 授权记录唯一标识符,数据库主键 最大长度100个字符 */
  private String id;

  /** 注册客户端ID,关联到 OAuth2 客户端 标识发起授权请求的客户端应用 最大长度100个字符 */
  private String registeredClientId;

  /** 用户主体名称,标识被授权的用户 通常为用户名或用户唯一标识 最大长度200个字符 */
  private String principalName;

  /**
   * 授权类型,标识使用的授权流程 可选值: authorization_code、client_credentials、refresh_token、device_code 等
   * 最大长度100个字符
   */
  private String authorizationGrantType;

  /** 已授权的权限范围集合,逗号分隔的权限字符串 记录用户实际授予客户端的权限 最大长度1000个字符 */
  private String authorizedScopes;

  /** 授权属性数据,存储授权相关的元数据 */
  private Map<String, Object> attributes;

  /** 授权状态信息,存储授权流程的状态数据 最大长度500个字符 */
  private String state;

  /** 授权码值,授权码模式下生成的临时授权码 */
  private String authorizationCodeValue;

  /** 授权码签发时间 使用 UTC 时区存储精确时间戳 */
  private Instant authorizationCodeIssuedAt;

  /** 授权码过期时间 超过此时间后授权码失效无法使用 */
  private Instant authorizationCodeExpiresAt;

  /** 授权码元数据,存储授权码相关的附加信息 */
  private Map<String, Object> authorizationCodeMetadata;

  /** 访问令牌值,用于访问受保护资源的凭证 */
  private String accessTokenValue;

  /** 访问令牌签发时间 使用 UTC 时区存储精确时间戳 */
  private Instant accessTokenIssuedAt;

  /** 访问令牌过期时间 超过此时间后访问令牌失效,需要使用刷新令牌获取新令牌 */
  private Instant accessTokenExpiresAt;

  /** 访问令牌元数据,存储访问令牌相关的附加信息 */
  private Map<String, Object> accessTokenMetadata;

  /** 访问令牌类型,标识令牌的使用方式 通常为 Bearer 类型 最大长度100个字符 */
  private String accessTokenType;

  /** 访问令牌关联的权限范围,逗号分隔的权限字符串 记录此访问令牌可以访问的资源范围 最大长度1000个字符 */
  private String accessTokenScopes;

  /** OpenID Connect ID令牌值,包含用户身份信息的JWT令牌 */
  private String oidcIdTokenValue;

  /** ID令牌签发时间 使用 UTC 时区存储精确时间戳 */
  private Instant oidcIdTokenIssuedAt;

  /** ID令牌过期时间 超过此时间后ID令牌失效 */
  private Instant oidcIdTokenExpiresAt;

  /** ID令牌元数据,存储ID令牌相关的附加信息 */
  private Map<String, Object> oidcIdTokenMetadata;

  /** 刷新令牌值,用于获取新访问令牌的长期凭证 */
  private String refreshTokenValue;

  /** 刷新令牌签发时间 使用 UTC 时区存储精确时间戳 */
  private Instant refreshTokenIssuedAt;

  /** 刷新令牌过期时间 超过此时间后刷新令牌失效,用户需要重新授权 */
  private Instant refreshTokenExpiresAt;

  /** 刷新令牌元数据,存储刷新令牌相关的附加信息 */
  private Map<String, Object> refreshTokenMetadata;

  /** 用户码值,设备授权流程中用户输入的验证码 */
  private String userCodeValue;

  /** 用户码签发时间 使用 UTC 时区存储精确时间戳 */
  private Instant userCodeIssuedAt;

  /** 用户码过期时间 超过此时间后用户码失效无法验证 */
  private Instant userCodeExpiresAt;

  /** 用户码元数据,存储用户码相关的附加信息 */
  private Map<String, Object> userCodeMetadata;

  /** 设备码值,设备授权流程中设备使用的验证码 */
  private String deviceCodeValue;

  /** 设备码签发时间 使用 UTC 时区存储精确时间戳 */
  private Instant deviceCodeIssuedAt;

  /** 设备码过期时间 超过此时间后设备码失效无法轮询 */
  private Instant deviceCodeExpiresAt;

  /** 设备码元数据,存储设备码相关的附加信息 */
  private Map<String, Object> deviceCodeMetadata;
}
