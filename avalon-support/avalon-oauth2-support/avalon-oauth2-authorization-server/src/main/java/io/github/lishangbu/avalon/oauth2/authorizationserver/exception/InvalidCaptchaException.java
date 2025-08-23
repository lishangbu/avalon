package io.github.lishangbu.avalon.oauth2.authorizationserver.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 验证码异常类 校验验证码异常时抛出
 *
 * @author lishangbu
 * @since 2025/8/21
 */
public class InvalidCaptchaException extends AuthenticationException {

  public InvalidCaptchaException(String msg) {
    super(msg);
  }
}
