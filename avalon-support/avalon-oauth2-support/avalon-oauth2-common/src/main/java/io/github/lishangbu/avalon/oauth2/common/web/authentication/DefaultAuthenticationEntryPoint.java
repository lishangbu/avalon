package io.github.lishangbu.avalon.oauth2.common.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;

/**
 * Used by {@link ExceptionTranslationFilter} to commence an authentication scheme.
 *
 * @author lishangbu
 * @since 2025/8/22
 */
public class DefaultAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) {
    JsonResponseWriter.writeFailedResponse(
        response,
        HttpStatus.UNAUTHORIZED,
        SecurityErrorResultCode.UNAUTHORIZED,
        authException.getMessage());
  }
}
