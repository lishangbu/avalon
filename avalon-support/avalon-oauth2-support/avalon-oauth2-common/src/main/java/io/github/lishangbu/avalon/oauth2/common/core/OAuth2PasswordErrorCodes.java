package io.github.lishangbu.avalon.oauth2.common.core;

/**
 * Standard error codes for password grant type defined by the OAuth 2.0 Authorization Framework.
 *
 * @author xuxiaowei
 * @since 2025/9/29
 */
public final class OAuth2PasswordErrorCodes {
  /** {@code invalid_username} 无效的用户 */
  public static final String INVALID_USERNAME = "invalid_username";

  /** {@code invalid_password} 无效的密码 */
  public static final String INVALID_PASSWORD = "invalid_password";

  /** {@code user_disabled} 用户被禁用 */
  public static final String USER_DISABLED = "user_disabled";

  /** {@code account_expired} 账户已过期 */
  public static final String ACCOUNT_EXPIRED = "account_expired";

  /** {@code account_locked} 账户已锁定 */
  public static final String ACCOUNT_LOCKED = "account_locked";

  /** {@code credentials_expired} 凭证已过期 */
  public static final String CREDENTIALS_EXPIRED = "credentials_expired";

  private OAuth2PasswordErrorCodes() {}
}
