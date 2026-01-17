package io.github.lishangbu.avalon.web;

import io.github.lishangbu.avalon.web.response.ApiResultResponseAdvice;
import io.github.lishangbu.avalon.web.result.ApiResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/// ApiResultResponseAdvice 单元测试
///
/// 验证响应体包装逻辑，确保字符串、普通对象和 ApiResult 类型均能正确处理
class ApiResultResponseAdviceTest {
  private JsonMapper jsonMapper;
  private ApiResultResponseAdvice advice;

  @BeforeEach
  void setUp() {
    jsonMapper = new JsonMapper();
    advice = new ApiResultResponseAdvice(jsonMapper);
  }

  /// 测试字符串类型响应体包装为 JSON 字符串
  @Test
  void testWrapApiResultWithString() throws Exception {
    String body = "hello";
    Object result = advice.beforeBodyWrite(body, null, null, null, null, null);
    ApiResult<String> expected = ApiResult.ok(body);
    String expectedJson = jsonMapper.writeValueAsString(expected);
    Assertions.assertEquals(expectedJson, result);
  }

  /// 测试普通对象类型响应体包装为 ApiResult
  @Test
  void testWrapApiResultWithObject() {
    Integer body = 123;
    Object result = advice.beforeBodyWrite(body, null, null, null, null, null);
    Assertions.assertInstanceOf(ApiResult.class, result);
    Assertions.assertEquals(123, ((ApiResult<?>) result).data());
  }

  /// 测试已包装的 ApiResult 类型直接返回
  @Test
  void testWrapApiResultWithApiResult() {
    ApiResult<String> apiResult = ApiResult.ok("test");
    Object result = advice.beforeBodyWrite(apiResult, null, null, null, null, null);
    Assertions.assertSame(apiResult, result);
  }
}
