package io.github.lishangbu.avalon.dufs.exception;

/// DUFS 通用异常基类
///
/// 所有 DUFS 相关异常应继承自该类以便统一捕获与处理
///
/// @author lishangbu
/// @since 2025/8/11
public class DufsException extends RuntimeException {

  public DufsException() {}

  public DufsException(String message) {
    super(message);
  }

  public DufsException(String message, Throwable cause) {
    super(message, cause);
  }

  public DufsException(Throwable cause) {
    super(cause);
  }

  public DufsException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
