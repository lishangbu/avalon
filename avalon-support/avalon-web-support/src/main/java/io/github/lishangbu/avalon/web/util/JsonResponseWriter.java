package io.github.lishangbu.avalon.web.util;

import io.github.lishangbu.avalon.json.util.JsonUtils;
import io.github.lishangbu.avalon.web.result.ApiResult;
import io.github.lishangbu.avalon.web.result.ErrorResultCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * json格式的response写入工具类
 *
 * @author lishangbu
 * @since 2025/8/23
 */
@Slf4j
@UtilityClass
public class JsonResponseWriter {

  public void writeSuccessResponse(HttpServletResponse response, Object data) {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpStatus.OK.value());
    try {
      response.getWriter().write(JsonUtils.toJson(ApiResult.ok(data)));
      response.getWriter().flush();
      response.getWriter().close();
    } catch (IOException e) {
      log.error("写入response失败", e);
      throw new RuntimeException(e);
    }
  }

  public void writeSuccessResponse(HttpServletResponse response) {
    writeSuccessResponse(response, null);
  }

  public void writeFailedResponse(
      HttpServletResponse response,
      HttpStatus httpStatus,
      ErrorResultCode errorResultCode,
      String... errorMessages) {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(httpStatus.value());
    try {
      response
          .getWriter()
          .write(JsonUtils.toJson(ApiResult.failed(errorResultCode, errorMessages)));
      response.getWriter().flush();
      response.getWriter().close();
    } catch (IOException e) {
      log.error("写入response失败", e);
      throw new RuntimeException(e);
    }
  }
}
