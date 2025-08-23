package io.github.lishangbu.avalon.oauth2.common.exception;

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode;
import io.github.lishangbu.avalon.web.result.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author lishangbu
 * @since 2018/8/30 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityExceptionHandler {

  /**
   * 访问被拒绝异常
   *
   * @param e the e
   * @return ResultBean
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResult<Void> handleAccessDeniedException(AccessDeniedException e) {
    log.error("授权异常:[{}]", e.getMessage(), e);
    return ApiResult.failed(SecurityErrorResultCode.FORBIDDEN, e.getMessage());
  }

  /**
   * 认证异常
   *
   * @param e 认证异常信息
   * @return
   */
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiResult<Void> handleAuthenticationException(AuthenticationException e) {
    log.error("认证异常:[{}]", e.getMessage(), e);
    return ApiResult.failed(SecurityErrorResultCode.UNAUTHORIZED, e.getMessage());
  }
}
