package io.github.lishangbu.avalon.security.constant;

import lombok.experimental.UtilityClass;

/**
 * JWT claim 常量
 *
 * @author lishangbu
 * @since 2025/4/8
 */
@UtilityClass
public class JwtClaimConstants {
  // region AK相关额外信息
  public final String USER_ID = "id";
  public final String AUTHORITIES = "authorities";
  // endregion
}
