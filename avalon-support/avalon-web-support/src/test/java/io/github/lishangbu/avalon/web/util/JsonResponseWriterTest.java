package io.github.lishangbu.avalon.web.util;

import io.github.lishangbu.avalon.json.autoconfiguration.JacksonAutoConfiguration;
import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode;
import io.github.lishangbu.avalon.web.result.ErrorResultCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * JsonResponseWriter 单元测试
 *
 * <p>验证成功和失败响应的写入逻辑，确保内容、状态码和异常处理均符合预期
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JacksonAutoConfiguration.class)
class JsonResponseWriterTest {
  private HttpServletResponse response;
  private StringWriter stringWriter;
  private PrintWriter printWriter;

  @BeforeEach
  void setUp() throws IOException {
    response = Mockito.mock(HttpServletResponse.class);
    stringWriter = new StringWriter();
    printWriter = new PrintWriter(stringWriter);
    Mockito.when(response.getWriter()).thenReturn(printWriter);
  }

  /** 测试成功响应写入，包含数据 */
  @Test
  void testWriteSuccessResponseWithData() {
    JsonResponseWriter.writeSuccessResponse(response, "ok");
    printWriter.flush();
    String result = stringWriter.toString();
    Assertions.assertTrue(result.contains("\"data\":\"ok\""));
    Mockito.verify(response).setStatus(HttpStatus.OK.value());
    Mockito.verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    Mockito.verify(response).setCharacterEncoding(StandardCharsets.UTF_8.name());
  }

  /** 测试成功响应写入，无数据 */
  @Test
  void testWriteSuccessResponseWithoutData() {
    JsonResponseWriter.writeSuccessResponse(response);
    printWriter.flush();
    String result = stringWriter.toString();
    Assertions.assertTrue(result.contains("\"data\":null"));
    Mockito.verify(response).setStatus(HttpStatus.OK.value());
  }

  /** 测试失败响应写入 */
  @Test
  void testWriteFailedResponse() {
    ErrorResultCode errorCode = DefaultErrorResultCode.SERVER_ERROR;
    JsonResponseWriter.writeFailedResponse(response, HttpStatus.BAD_REQUEST, errorCode, "错误信息");
    printWriter.flush();
    String result = stringWriter.toString();
    Assertions.assertTrue(result.contains(Integer.toString(500)));
    Assertions.assertTrue(result.contains("错误信息"));
    Mockito.verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
  }

  /** 测试写入异常处理 */
  @Test
  void testWriteSuccessResponseIOException() throws IOException {
    HttpServletResponse errorResponse = Mockito.mock(HttpServletResponse.class);
    Mockito.when(errorResponse.getWriter()).thenThrow(new IOException("mock error"));
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          JsonResponseWriter.writeSuccessResponse(errorResponse, "fail");
        });
  }
}
