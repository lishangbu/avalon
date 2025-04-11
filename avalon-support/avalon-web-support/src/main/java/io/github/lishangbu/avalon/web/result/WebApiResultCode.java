package io.github.lishangbu.avalon.web.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * web api 结果
 *
 * @author lishangbu
 * @since 2025/4/8
 */
@RequiredArgsConstructor
@Getter
public enum WebApiResultCode implements ResultCode {
  /** 操作成功 */
  SUCCESS(200, "Success"),
  /** 参数错误 */
  BAD_REQUEST(400, "Bad Request"),

  /** 资源不存在 */
  RESOURCE_NOT_FOUND(404, "Resource Not Found"),

  /** 方法不支持 */
  METHOD_NOT_ALLOWED(405, "METHOD NOT ALLOWED"),

  /** 服务器内部错误 */
  SERVER_ERROR(500, "Internal Server Error");

  private final Integer code;

  private final String message;
}
