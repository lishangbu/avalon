package io.github.lishangbu.avalon.security.constant;

/**
 * JWT claim 常量
 *
 * @author lishangbu
 * @since 2025/4/8
 */
public final class JwtClaimConstants {
  // region AK相关额外信息
  public static final String USER_ID = "id";
  public static final String AUTHORITIES = "authorities";

  private JwtClaimConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
  // endregion
}
