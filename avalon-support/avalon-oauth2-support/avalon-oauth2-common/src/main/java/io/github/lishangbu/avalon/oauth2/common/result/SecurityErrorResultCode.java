package io.github.lishangbu.avalon.oauth2.common.result;

import io.github.lishangbu.avalon.web.result.ErrorResultCode;

/// 安全模块使用的默认错误码
///
/// 提供常见的 HTTP 授权/认证相关错误码定义
///
/// @author lishangbu
/// @since 2025/4/8
public enum SecurityErrorResultCode implements ErrorResultCode {

  /// 401 Unauthorized
  ///
  /// @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1: Authentication</a>
  UNAUTHORIZED(401, "Unauthorized"),

  /// 403 Forbidden
  ///
  /// @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1: Semantics and
  // Content</a>
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
