package io.github.lishangbu.avalon.security.exception;

/**
 * 找不到JWT异常
 *
 * @author lishangbu
 * @since 2025/4/10
 */
public class JsonWebTokenNotFoundException extends RuntimeException {
  public JsonWebTokenNotFoundException() {
    this(null, null);
  }

  public JsonWebTokenNotFoundException(final String message) {
    this(message, null);
  }

  public JsonWebTokenNotFoundException(final Throwable cause) {
    this(cause != null ? cause.getMessage() : null, cause);
  }

  public JsonWebTokenNotFoundException(final String message, final Throwable cause) {
    super(message);
    if (cause != null) super.initCause(cause);
  }
}
