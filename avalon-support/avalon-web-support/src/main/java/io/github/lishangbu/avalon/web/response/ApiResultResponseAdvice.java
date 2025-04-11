package io.github.lishangbu.avalon.web.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.web.result.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * API响应结果包装
 *
 * @author lishangbu
 * @since 2023/5/1
 */
@RequiredArgsConstructor
@RestControllerAdvice(basePackages = "io.github.lishangbu.avalon")
public class ApiResultResponseAdvice implements ResponseBodyAdvice<Object> {

  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  @SneakyThrows
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    return wrapApiResult(body);
  }

  /**
   * 包装API调用结果
   *
   * @param body
   * @return
   * @throws JsonProcessingException
   */
  private Object wrapApiResult(Object body) throws JsonProcessingException {
    if (body instanceof String) {
      return objectMapper.writeValueAsString(ApiResult.ok(body));
    }

    if (body instanceof ApiResult apiResult) {
      return apiResult;
    }

    return ApiResult.ok(body);
  }
}
