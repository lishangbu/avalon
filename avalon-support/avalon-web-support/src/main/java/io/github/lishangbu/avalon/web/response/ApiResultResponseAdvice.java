package io.github.lishangbu.avalon.web.response;

import io.github.lishangbu.avalon.json.exception.JsonProcessingRuntimeException;
import io.github.lishangbu.avalon.web.result.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * API响应结果包装
 *
 * @author lishangbu
 * @since 2023/5/1
 */
@RestControllerAdvice(basePackages = "io.github.lishangbu.avalon")
public class ApiResultResponseAdvice implements ResponseBodyAdvice<Object> {

  private static final Logger log = LoggerFactory.getLogger(ApiResultResponseAdvice.class);
  private final ObjectMapper objectMapper;

  public ApiResultResponseAdvice(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    try {
      return wrapApiResult(body);
    } catch (JacksonException e) {
      log.error("ApiResultResponse JsonProcessingException:[{}]", e.getMessage());
      throw new JsonProcessingRuntimeException(e.getMessage());
    }
  }

  /**
   * 包装API调用结果
   *
   * @param body
   * @return
   * @throws JacksonException
   */
  private Object wrapApiResult(Object body) throws JacksonException {
    if (body instanceof String) {
      return objectMapper.writeValueAsString(ApiResult.ok(body));
    }

    if (body instanceof ApiResult apiResult) {
      return apiResult;
    }

    return ApiResult.ok(body);
  }
}
