package io.github.lishangbu.avalon.oauth2.common.web.authentication;

import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 默认认证成功处理器
 *
 * @author lishangbu
 * @since 2025/8/23
 */
@Slf4j
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    JsonResponseWriter.writeSuccessResponse(response);
  }
}
