package io.github.lishangbu.avalon.oauth2.common.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;


/**
 * 统一的认证入口点，用于在未认证或认证失败时返回统一的 JSON 错误响应
 *
 * <p>此类由 {@link ExceptionTranslationFilter} 在需要发起认证流程时调用（commence）， 返回 HTTP 401
 * 状态以及统一的业务错误码，便于前端和网关识别和处理
 *
 * @author lishangbu
 * @since 2025/8/22
 */
@Slf4j
public class DefaultAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
    HttpServletRequest request,
    HttpServletResponse response,
    AuthenticationException authException) {
    log.error(
      "AuthenticationEntryPoint invoked for request [{}], reason=[{}]",
      request.getRequestURI(),
      authException.getMessage());

    JsonResponseWriter.writeFailedResponse(
      response,
      HttpStatus.UNAUTHORIZED,
      SecurityErrorResultCode.UNAUTHORIZED,
      authException.getMessage());
  }
}
