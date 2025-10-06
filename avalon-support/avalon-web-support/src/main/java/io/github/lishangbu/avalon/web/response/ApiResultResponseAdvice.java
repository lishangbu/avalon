package io.github.lishangbu.avalon.web.response;

import io.github.lishangbu.avalon.web.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

/**
 * API响应结果包装增强器，统一封装所有 Controller 返回值为 ApiResult
 *
 * <p>支持自动包装普通对象、字符串类型和已包装的 ApiResult 类型 字符串类型特殊处理，保证响应内容为标准 JSON 格式 适用于 io.github.lishangbu.avalon
 * 包下所有 RestController
 *
 * @author lishangbu
 * @since 2023/5/1
 */
@RequiredArgsConstructor
@RestControllerAdvice(basePackages = "io.github.lishangbu.avalon")
public class ApiResultResponseAdvice implements ResponseBodyAdvice<Object> {

  private final JsonMapper jsonMapper;

  /**
   * 判断是否需要处理响应体
   *
   * <p>所有类型均支持包装
   *
   * @param returnType 方法返回类型参数
   * @param converterType 消息转换器类型
   * @return 总是返回 true，表示所有响应均处理
   */
  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  /**
   * 响应体写出前的统一包装处理
   *
   * <p>将原始响应体包装为 ApiResult，字符串类型特殊处理为 JSON 字符串
   *
   * @param body 原始响应体
   * @param returnType 方法返回类型参数
   * @param selectedContentType 响应内容类型
   * @param selectedConverterType 消息转换器类型
   * @param request 当前请求对象
   * @param response 当前响应对象
   * @return 包装后的响应体
   */
  @Override
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
   * <p>普通对象和字符串类型均包装为 ApiResult，字符串类型返回 JSON 字符串 已包装的 ApiResult 类型直接返回
   *
   * @param body 原始响应体
   * @return 包装后的 ApiResult 或 JSON 字符串
   */
  private Object wrapApiResult(Object body) {
    if (body instanceof String) {
      return jsonMapper.writeValueAsString(ApiResult.ok(body));
    }

    if (body instanceof ApiResult apiResult) {
      return apiResult;
    }

    return ApiResult.ok(body);
  }
}
