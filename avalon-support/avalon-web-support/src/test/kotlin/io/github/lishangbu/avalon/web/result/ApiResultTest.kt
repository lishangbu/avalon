package io.github.lishangbu.avalon.web.result

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * [ApiResult] 测试
 *
 * 验证成功和失败结果的状态码、数据与错误信息
 */
class ApiResultTest {
    @Test
    fun testOkWithData() {
        val testData = "Test Data"
        val result = ApiResult.ok(testData)

        assertEquals(200, result.code)
        assertEquals(testData, result.data)
        assertNull(result.errorMessage)
    }

    @Test
    fun testOkWithNullData() {
        val result = ApiResult.ok<Void>(null)

        assertEquals(200, result.code)
        assertNull(result.data)
        assertNull(result.errorMessage)
    }

    @Test
    fun testFailedWithCodeAndMessage() {
        val errorCode = 500
        val errorMessage = "Internal Server Error"
        val result = ApiResult.failed(errorCode, errorMessage)

        assertEquals(errorCode, result.code)
        assertEquals(errorMessage, result.errorMessage)
        assertNull(result.data)
    }

    @Test
    fun testFailedWithErrorEnum() {
        val errorCode: ErrorResultCode = DefaultErrorResultCode.SERVER_ERROR
        val additionalMessage = "Database connection failed"
        val result = ApiResult.failed(errorCode, additionalMessage)

        assertEquals(errorCode.code(), result.code)
        assertEquals(additionalMessage, result.errorMessage)
        assertNull(result.data)
    }

    @Test
    fun testFailedWithEmptyErrorMessages() {
        val errorCode: ErrorResultCode = DefaultErrorResultCode.RESOURCE_NOT_FOUND
        val result = ApiResult.failed(errorCode)

        assertEquals(errorCode.code(), result.code)
        assertEquals(errorCode.errorMessage(), result.errorMessage)
        assertNull(result.data)
    }

    @Test
    fun testFailedWithSomeErrorMessages() {
        val errorCode: ErrorResultCode = DefaultErrorResultCode.METHOD_NOT_ALLOWED
        val result =
            ApiResult.failed(errorCode, "GET_METHOD_NOT_ALLOWED", "POST_METHOD_NOT_ALLOWED")

        assertEquals(errorCode.code(), result.code)
        assertEquals("GET_METHOD_NOT_ALLOWED,POST_METHOD_NOT_ALLOWED", result.errorMessage)
        assertNull(result.data)
    }
}
