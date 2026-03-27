package io.github.lishangbu.avalon.web.result

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ApiResultTest {
    @Test
    fun okBuildsSuccessfulResponseAndAccessorsMirrorProperties() {
        val result = ApiResult.ok("payload")

        assertEquals(ApiResult.SUCCESS_CODE, result.code)
        assertEquals("payload", result.data)
        assertNull(result.errorMessage)
        assertEquals(ApiResult.SUCCESS_CODE, result.code())
        assertEquals("payload", result.data())
        assertNull(result.errorMessage())
    }

    @Test
    fun failedBuildsResponseFromExplicitCodeAndMessage() {
        val result = ApiResult.failed(418, "teapot")

        assertEquals(418, result.code)
        assertNull(result.data)
        assertEquals("teapot", result.errorMessage)
    }

    @Test
    fun failedUsesDefaultEnumMessageWhenNoCustomMessagesProvided() {
        val result = ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR)

        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), result.code)
        assertEquals(DefaultErrorResultCode.SERVER_ERROR.errorMessage(), result.errorMessage)
    }

    @Test
    fun failedJoinsCustomMessagesWhenProvided() {
        val result = ApiResult.failed(DefaultErrorResultCode.BAD_REQUEST, "field 1", "field 2")

        assertEquals(DefaultErrorResultCode.BAD_REQUEST.code(), result.code)
        assertEquals("field 1,field 2", result.errorMessage)
    }
}
