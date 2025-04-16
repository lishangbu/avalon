package io.github.lishangbu.avalon.security.exception;

import io.github.lishangbu.avalon.security.result.WebSecurityResultCode;
import io.github.lishangbu.avalon.web.result.ApiResult;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 安全异常处理器
 *
 * @author lishangbu
 * @since 2018/8/30
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

  /**
   * 未携带JWT异常
   *
   * @param e the e
   * @return ResultBean
   */
  @ExceptionHandler(JsonWebTokenNotFoundException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiResult<Void> handleJsonWebTokenNotFoundException(JsonWebTokenNotFoundException e) {
    return ApiResult.failed(WebSecurityResultCode.UNAUTHORIZED, "需要登录");
  }

  /**
   * 处理用户输入导致的错误用户名和密码
   *
   * @return 包含了API调用结果的错误处理
   */
  @ExceptionHandler(value = {BadCredentialsException.class, UsernameNotFoundException.class})
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiResult<Void> handleUserInputException(AuthenticationException e) {
    log.error("User input cause exception:[{}]", e.getMessage());
    return ApiResult.failed(WebSecurityResultCode.UNAUTHORIZED, "用户名或密码错误");
  }

  /**
   * 认证异常处理
   *
   * @param e the e
   * @return ResultBean
   */
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiResult<Void> handleAuthenticationException(AuthenticationException e) {
    log.error("认证异常信息:[{}]", e.getMessage());
    return ApiResult.failed(WebSecurityResultCode.UNAUTHORIZED, e.getMessage());
  }

  /**
   * 授权异常.
   *
   * @param e the e
   * @return ResultBean
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResult<Void> handleAccessDeniedException(AccessDeniedException e) {
    log.error("访问异常:[{}]", e.getMessage());
    return ApiResult.failed(WebSecurityResultCode.FORBIDDEN, e.getMessage());
  }

  /**
   * 处理JWT过期异常
   *
   * @param e JWT时间过期
   * @return 包装JWT过期异常信息的Api调用结果
   */
  @ExceptionHandler(ExpiredJwtException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResult<Void> handleExpiredJwtException(ExpiredJwtException e) {
    log.error("JWT令牌过期:[{}]", e.getMessage());
    return ApiResult.failed(WebSecurityResultCode.EXPIRED_JWT, e.getMessage());
  }
}
