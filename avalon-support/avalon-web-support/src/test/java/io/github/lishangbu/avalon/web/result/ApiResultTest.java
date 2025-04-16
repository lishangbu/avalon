package io.github.lishangbu.avalon.web.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author lishangbu
 * @since 2025/4/16
 */
public class ApiResultTest {

  @Test
  public void testOkWithData() {
    String testData = "Test Data";

    ApiResult<String> result = ApiResult.ok(testData);

    // 验证返回的状态码为 200
    assertEquals(200, result.code());

    // 验证返回的数据为传入的 testData
    assertEquals(testData, result.data());

    // 验证错误信息为 null
    assertNull(result.errorMessage());
  }

  @Test
  public void testOkWithNullData() {
    ApiResult<Void> result = ApiResult.ok(null);

    // 验证返回的状态码为 200
    assertEquals(200, result.code());

    // 验证返回的数据为 null
    assertNull(result.data());

    // 验证错误信息为 null
    assertNull(result.errorMessage());
  }

  @Test
  public void testFailedWithCodeAndMessage() {
    int errorCode = 500;
    String errorMessage = "Internal Server Error";

    ApiResult<Void> result = ApiResult.failed(errorCode, errorMessage);

    // 验证返回的状态码为指定的错误码
    assertEquals(errorCode, result.code());

    // 验证错误信息为指定的错误信息
    assertEquals(errorMessage, result.errorMessage());

    // 验证数据为 null
    assertNull(result.data());
  }

  @Test
  public void testFailedWithErrorEnum() {
    ErrorResultCode errorCode = DefaultErrorResultCode.SERVER_ERROR;
    String additionalMessage = "Database connection failed";

    ApiResult<Void> result = ApiResult.failed(errorCode, additionalMessage);

    // 验证返回的状态码为指定的错误码
    assertEquals(errorCode.code(), result.code());

    // 验证错误信息为联合错误信息
    assertEquals(additionalMessage, result.errorMessage());

    // 验证数据为 null
    assertNull(result.data());
  }

  @Test
  public void testFailedWithEmptyErrorMessages() {
    ErrorResultCode errorCode = DefaultErrorResultCode.RESOURCE_NOT_FOUND;

    ApiResult<Void> result = ApiResult.failed(errorCode);

    // 验证返回的状态码为指定的错误码
    assertEquals(errorCode.code(), result.code());

    // 验证错误信息为错误码对应的默认消息
    assertEquals(errorCode.errorMessage(), result.errorMessage());

    // 验证数据为 null
    assertNull(result.data());
  }

  @Test
  public void testFailedWithSomeErrorMessages() {
    ErrorResultCode errorCode = DefaultErrorResultCode.METHOD_NOT_ALLOWED;

    ApiResult<Void> result =
        ApiResult.failed(errorCode, "GET_METHOD_NOT_ALLOWED", "POST_METHOD_NOT_ALLOWED");

    // 验证返回的状态码为指定的错误码
    assertEquals(errorCode.code(), result.code());

    // 验证错误信息为自定义的信息，并且通过,分隔
    assertEquals("GET_METHOD_NOT_ALLOWED,POST_METHOD_NOT_ALLOWED", result.errorMessage());

    // 验证数据为 null
    assertNull(result.data());
  }
}
