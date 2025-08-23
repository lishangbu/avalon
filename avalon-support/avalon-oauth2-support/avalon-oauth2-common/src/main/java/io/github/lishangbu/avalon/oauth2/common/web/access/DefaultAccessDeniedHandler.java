package io.github.lishangbu.avalon.oauth2.common.web.access;

import io.github.lishangbu.avalon.json.util.JsonUtils;
import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode;
import io.github.lishangbu.avalon.web.result.ApiResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 默认异常处理
 *
 * @author lishangbu
 * @since 2025/8/22
 */
public class DefaultAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            JsonUtils.toJson(
                ApiResult.failed(
                    SecurityErrorResultCode.FORBIDDEN, accessDeniedException.getMessage())));
    response.getWriter().flush();
  }
}
