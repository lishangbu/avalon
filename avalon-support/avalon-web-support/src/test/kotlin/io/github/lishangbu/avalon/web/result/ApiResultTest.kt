package io.github.lishangbu.avalon.web.result

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * ApiResult 单元测试 覆盖 ApiResult 的成功与失败返回情况，验证状态码、数据与错误信息的语义
 *
 * @author lishangbu
 * @since 2025/4/16 验证返回的状态码为 200 验证返回的数据为传入的 testData 验证错误信息为 null 验证返回的数据为 null 验证返回的状态码为指定的错误码
 *   验证错误信息为指定的错误信息 验证数据为 null 验证错误信息为联合错误信息 验证错误信息为错误码对应的默认消息 验证错误信息为自定义的信息，并且通过,分隔 // 验证返回的状态码为
 *   200 // 验证返回的数据为传入的 testData // 验证错误信息为 null // 验证返回的数据为 null // 验证返回的状态码为指定的错误码 //
 *   验证错误信息为指定的错误信息 // 验证数据为 null // 验证错误信息为联合错误信息 // 验证错误信息为错误码对应的默认消息 // 验证错误信息为自定义的信息，并且通过,分隔
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
