package io.github.lishangbu.avalon.web

import io.github.lishangbu.avalon.web.response.ApiResultResponseAdvice
import io.github.lishangbu.avalon.web.result.ApiResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import tools.jackson.databind.json.JsonMapper

/**
 * ApiResultResponseAdvice 单元测试 验证响应体包装逻辑，确保字符串、普通对象和 ApiResult 类型均能正确处理 测试字符串类型响应体包装为 JSON 字符串
 * 测试普通对象类型响应体包装为 ApiResult 测试已包装的 ApiResult 类型直接返回
 */
class ApiResultResponseAdviceTest {
    private lateinit var jsonMapper: JsonMapper
    private lateinit var advice: ApiResultResponseAdvice

    @BeforeEach
    fun setUp() {
        jsonMapper = JsonMapper()
        advice = ApiResultResponseAdvice(jsonMapper)
    }

    @Test
    fun testWrapApiResultWithString() {
        val body = "hello"
        val result = beforeBodyWrite(body)
        val expectedJson = jsonMapper.writeValueAsString(ApiResult.ok(body))

        assertEquals(expectedJson, result)
    }

    @Test
    fun testWrapApiResultWithObject() {
        val result = beforeBodyWrite(123)

        assertInstanceOf(ApiResult::class.java, result)
        assertEquals(123, (result as ApiResult<*>).data)
    }

    @Test
    fun testWrapApiResultWithApiResult() {
        val apiResult = ApiResult.ok("test")
        val result = beforeBodyWrite(apiResult)

        assertSame(apiResult, result)
    }

    private fun beforeBodyWrite(body: Any?): Any? =
        advice.beforeBodyWrite(
            body,
            Mockito.mock(MethodParameter::class.java),
            MediaType.APPLICATION_JSON,
            HttpMessageConverter::class.java as Class<out HttpMessageConverter<*>>,
            Mockito.mock(ServerHttpRequest::class.java),
            Mockito.mock(ServerHttpResponse::class.java),
        )
}
