package io.github.lishangbu.avalon.web.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DefaultErrorResultCode 单元测试
 */
public class DefaultErrorResultCodeTest {

  @Test
  public void testCodeAndErrorMessage() {
    // 验证 BAD_REQUEST 枚举
    assertEquals(400, DefaultErrorResultCode.BAD_REQUEST.code());
    assertEquals("Bad Request", DefaultErrorResultCode.BAD_REQUEST.errorMessage());

    // 验证 RESOURCE_NOT_FOUND 枚举
    assertEquals(404, DefaultErrorResultCode.RESOURCE_NOT_FOUND.code());
    assertEquals("Resource Not Found", DefaultErrorResultCode.RESOURCE_NOT_FOUND.errorMessage());

    // 验证 METHOD_NOT_ALLOWED 枚举
    assertEquals(405, DefaultErrorResultCode.METHOD_NOT_ALLOWED.code());
    assertEquals("METHOD NOT ALLOWED", DefaultErrorResultCode.METHOD_NOT_ALLOWED.errorMessage());

    // 验证 SERVER_ERROR 枚举
    assertEquals(500, DefaultErrorResultCode.SERVER_ERROR.code());
    assertEquals("Internal Server Error", DefaultErrorResultCode.SERVER_ERROR.errorMessage());
  }
}
