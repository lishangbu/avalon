package io.github.lishangbu.avalon.web.result;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.util.ObjectUtils;

/// API 返回结果的封装类，包含状态码、数据和错误信息
///
/// @param code         获取 API 返回结果的状态码
/// @param data         获取 API 返回的数据
/// @param errorMessage 获取 API 返回的错误信息
/// @param <T>          返回的数据类型
/// @author lishangbu
/// @since 2025/4/16
public record ApiResult<T>(Integer code, T data, String errorMessage) implements Serializable {

  /// 默认的成功操作码，通常用于表示操作成功
  public static final Integer SUCCESS_CODE = 200;
  /// 用于序列化的静态常量，表示序列化版本 UID
  @Serial private static final long serialVersionUID = 1L;

  /// 创建一个成功的 API 返回结果
  ///
  /// @param data 返回的数据
  /// @param <T>  数据类型
  /// @return 成功的 API 返回结果
  public static <T> ApiResult<T> ok(T data) {
    return new ApiResult<>(SUCCESS_CODE, data, null);
  }

  /// 创建一个失败的 API 返回结果，带有指定的错误码和错误信息
  ///
  /// @param code         错误码
  /// @param errorMessage 错误信息
  /// @return 失败的 API 返回结果
  public static ApiResult<Void> failed(int code, String errorMessage) {
    return new ApiResult<>(code, null, errorMessage);
  }

  /// 构建一个失败的 API 返回结果
  ///
  /// 根据传入的错误结果码和错误信息生成一个失败的 API 返回结果。如果没有提供额外的错误信息，
  /// 则使用错误结果码中预设的错误信息。若提供了额外的错误信息，则会将其与错误结果码的错误信息一同返回
  /// @param errorResultCode 错误结果码，包含错误的状态码和默认的错误信息
  /// @param errorMessages 可变长度的额外错误信息（可选）
  /// @return 返回一个封装了失败状态的 API 结果对象
  public static ApiResult<Void> failed(ErrorResultCode errorResultCode, String... errorMessages) {
    return ObjectUtils.isEmpty(errorMessages)
        ? failed(errorResultCode.code(), errorResultCode.errorMessage())
        : failed(errorResultCode.code(), String.join(",", errorMessages));
  }
}
