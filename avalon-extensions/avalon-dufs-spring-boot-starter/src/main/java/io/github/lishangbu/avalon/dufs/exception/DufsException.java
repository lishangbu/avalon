package io.github.lishangbu.avalon.dufs.exception;

/**
 * DUFS 异常信息
 *
 * @author lishangbu
 * @since 2025/8/11
 */
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
