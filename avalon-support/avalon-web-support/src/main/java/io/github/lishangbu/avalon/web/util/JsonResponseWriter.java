package io.github.lishangbu.avalon.web.util;

import io.github.lishangbu.avalon.web.result.ApiResult;
import io.github.lishangbu.avalon.web.result.ErrorResultCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/// JSON 格式的 response 写入工具类
///
/// 提供写入成功与失败响应的便捷方法，自动设置字符编码/Content-Type/状态码
///
/// @author lishangbu
/// @since 2025/8/23
@Slf4j
@UtilityClass
public class JsonResponseWriter {

    public void writeSuccessResponse(
            HttpServletResponse response, JsonMapper jsonMapper, Object data) {
        Objects.requireNonNull(jsonMapper, "jsonMapper");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        try {
            response.getWriter().write(jsonMapper.writeValueAsString(ApiResult.ok(data)));
            response.getWriter().flush();
            response.getWriter().close();
        } catch (IOException e) {
            log.error("写入response失败", e);
            throw new RuntimeException(e);
        }
    }

    public void writeSuccessResponse(HttpServletResponse response, JsonMapper jsonMapper) {
        writeSuccessResponse(response, jsonMapper, null);
    }

    public void writeFailedResponse(
            HttpServletResponse response,
            JsonMapper jsonMapper,
            HttpStatus httpStatus,
            ErrorResultCode errorResultCode,
            String... errorMessages) {
        Objects.requireNonNull(jsonMapper, "jsonMapper");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(httpStatus.value());
        try {
            response.getWriter()
                    .write(jsonMapper.writeValueAsString(ApiResult.failed(errorResultCode, errorMessages)));
            response.getWriter().flush();
            response.getWriter().close();
        } catch (IOException e) {
            log.error("写入response失败", e);
            throw new RuntimeException(e);
        }
    }
}
