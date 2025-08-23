package io.github.lishangbu.avalon.oauth2.common.result;

import io.github.lishangbu.avalon.web.result.ErrorResultCode;

/**
 * web api 结果
 *
 * @author lishangbu
 * @since 2025/4/8
 */
public enum SecurityErrorResultCode implements ErrorResultCode {

  /**
   * {@code 401 Unauthorized}.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1: Authentication,
   *     section 3.1</a>
   */
  UNAUTHORIZED(401, "Unauthorized"),

  /**
   * {@code 403 Forbidden}.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1: Semantics and
   *     Content, section 6.5.3</a>
   */
  FORBIDDEN(403, "Forbidden");

  private final Integer code;

  private final String message;

  SecurityErrorResultCode(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer code() {
    return this.code;
  }

  public String errorMessage() {
    return this.message;
  }
}
