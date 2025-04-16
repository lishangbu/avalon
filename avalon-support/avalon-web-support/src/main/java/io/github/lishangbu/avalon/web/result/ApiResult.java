package io.github.lishangbu.avalon.web.result;

import java.io.Serial;
import java.io.Serializable;

/**
 * 响应信息主体类，用于封装API响应数据，包括状态码、信息和数据。
 *
 * @param <T> 响应数据的类型
 * @author lishangbu
 * @since 2025/4/16
 */
public record ApiResult<T>(
    /** 状态码 */
    int code,
    /** 错误信息 */
    String errorMessage,
    /** 数据 */
    T data)
    implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * 创建一个成功的ApiResult实例，不携带数据
   *
   * @param <T> 响应数据的类型
   * @return 成功的ApiResult实例
   */
  public static <T> ApiResult<T> empty() {
    return ok(null);
  }

  /**
   * 创建一个成功的ApiResult实例，携带数据
   *
   * @param <T> 响应数据的类型
   * @param data 响应数据
   * @return 成功的ApiResult实例
   */
  public static <T> ApiResult<T> ok(T data) {
    return create(WebApiResultCode.SUCCESS.getCode(), WebApiResultCode.SUCCESS.getMessage(), data);
  }

  /**
   * 创建一个失败的ApiResult实例，携带状态码和信息
   *
   * @param <T> 响应数据的类型
   * @param code 失败的状态码
   * @param errorMessage 失败的信息
   * @return 失败的ApiResult实例
   */
  public static <T> ApiResult<T> failed(int code, String errorMessage) {
    return create(code, errorMessage, null);
  }

  /**
   * 创建一个失败的ApiResult实例，基于给定的ResultCode
   *
   * @param <T> 响应数据的类型
   * @param failResult 失败的ResultCode
   * @return 失败的ApiResult实例
   */
  public static <T> ApiResult<T> failed(ResultCode failResult) {
    return new ApiResult<>(failResult.getCode(), failResult.getMessage(), null);
  }

  /**
   * 创建一个失败的ApiResult实例，基于给定的ResultCode和自定义信息
   *
   * @param <T> 响应数据的类型
   * @param failResult 失败的ResultCode
   * @param message 自定义的失败信息
   * @return 失败的ApiResult实例
   */
  public static <T> ApiResult<T> failed(ResultCode failResult, String message) {
    return new ApiResult<>(failResult.getCode(), message, null);
  }

  /**
   * 创建一个带有指定状态码、信息和数据的ApiResult实例
   *
   * @param <T> 响应数据的类型
   * @param code 状态码
   * @param message 信息
   * @param data 数据
   * @return ApiResult实例
   */
  public static <T> ApiResult<T> create(int code, String message, T data) {
    return new ApiResult<>(code, message, data);
  }
}
