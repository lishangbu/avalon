package io.github.lishangbu.avalon.authorization.constant;

/**
 * 缓存常量
 *
 * @author lishangbu
 * @since 2025/8/23
 */
public abstract class AuthorizationCacheConstants {

  public static final String CAFFEINE_CACHE_BEAN_NAME = "authorizationCaffeineCacheManager";

  /** oauth2 认证主体缓存 */
  public static final String OAUTH_2_AUTHORIZATION_CACHE = "oauth2AuthorizationCache";

  /** oauth2 认证客户端缓存(通过ID) */
  public static final String OAUTH_2_REGISTERED_CLIENT_CACHE_BY_ID =
      "oauth2RegisteredClientCache:id:";

  /** oauth2 认证客户端缓存(通过CLIENT_ID) */
  public static final String OAUTH_2_REGISTERED_CLIENT_CACHE_BY_CLIENT_ID =
      "oauth2RegisteredClientCache:clientId:";
}
