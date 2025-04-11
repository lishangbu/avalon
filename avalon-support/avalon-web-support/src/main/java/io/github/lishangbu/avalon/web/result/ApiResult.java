package io.github.lishangbu.avalon.web.result;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 响应信息主体
 *
 * @param <T>
 * @author Hccake
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResult<T> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 返回状态码 */
  private int code;

  /** 返回信息 */
  private String message;

  /** 返回数据 */
  private T data;

  public static <T> ApiResult<T> ok() {
    return ok(null);
  }

  public static <T> ApiResult<T> ok(T data) {
    return ok(data, WebApiResultCode.SUCCESS.getMessage());
  }

  public static <T> ApiResult<T> ok(T data, String message) {
    return create(WebApiResultCode.SUCCESS.getCode(), message, data);
  }

  public static <T> ApiResult<T> failed(int code, String message) {
    return ApiResult.<T>builder().code(code).message(message).build();
  }

  public static <T> ApiResult<T> failed(ResultCode failResult) {
    return ApiResult.<T>builder()
        .code(failResult.getCode())
        .message(failResult.getMessage())
        .build();
  }

  public static <T> ApiResult<T> failed(ResultCode failResult, String message) {
    return ApiResult.<T>builder().code(failResult.getCode()).message(message).build();
  }

  public static <T> ApiResult<T> create(int code, String message, T data) {
    return ApiResult.<T>builder().code(code).message(message).data(data).build();
  }
}
