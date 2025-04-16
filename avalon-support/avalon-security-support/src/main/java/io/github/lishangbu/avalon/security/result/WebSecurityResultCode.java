package io.github.lishangbu.avalon.security.result;

import io.github.lishangbu.avalon.web.result.ResultCode;

/**
 * 安全状态结果
 *
 * @author lishangbu
 * @since 2025/4/8
 */
public enum WebSecurityResultCode implements ResultCode {

  /**
   * 未认证
   *
   * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1: Authentication,
   *     section 3.1</a>
   */
  UNAUTHORIZED(401, "Unauthorized"),
  /**
   * 未授权
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1: Semantics and
   *     Content, section 6.5.3</a>
   */
  FORBIDDEN(403, "Forbidden"),

  /** JWT过期 */
  EXPIRED_JWT(40101, "Expired Jwt");

  private final Integer code;

  private final String message;

  WebSecurityResultCode(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() {
    return this.code;
  }

  public String getMessage() {
    return this.message;
  }
}
