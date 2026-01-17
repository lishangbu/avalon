package io.github.lishangbu.avalon.web.result;

/// web api 结果默认错误码
///
/// 定义常见的 HTTP 错误码与默认错误信息
///
/// @author lishangbu
/// @since 2025/4/8
public enum DefaultErrorResultCode implements ErrorResultCode {

  /// 参数错误
  BAD_REQUEST(400, "Bad Request"),

  /// 资源不存在
  RESOURCE_NOT_FOUND(404, "Resource Not Found"),

  /// 方法不支持
  METHOD_NOT_ALLOWED(405, "METHOD NOT ALLOWED"),

  /// 服务器内部错误
  SERVER_ERROR(500, "Internal Server Error");

  private final Integer code;

  private final String message;

  DefaultErrorResultCode(Integer code, String message) {
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
