package io.github.lishangbu.avalon.web.util

import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode
import io.github.lishangbu.avalon.web.result.ErrorResultCode
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets

/**
 * JsonResponseWriter 单元测试 验证成功和失败响应的写入逻辑，确保内容、状态码和异常处理均符合预期 测试成功响应写入，包含数据 测试成功响应写入，无数据 测试失败响应写入
 * 测试写入异常处理
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JacksonAutoConfiguration::class])
class JsonResponseWriterTest {
    @Autowired
    private lateinit var jsonMapper: JsonMapper

    private lateinit var response: HttpServletResponse
    private lateinit var stringWriter: StringWriter
    private lateinit var printWriter: PrintWriter

    @BeforeEach
    fun setUp() {
        response = Mockito.mock(HttpServletResponse::class.java)
        stringWriter = StringWriter()
        printWriter = PrintWriter(stringWriter)
        Mockito.`when`(response.writer).thenReturn(printWriter)
    }

    @Test
    fun testWriteSuccessResponseWithData() {
        JsonResponseWriter.writeSuccessResponse(response, jsonMapper, "ok")
        printWriter.flush()
        val result = stringWriter.toString()

        assertTrue(result.contains("\"data\":\"ok\""))
        Mockito.verify(response).status = HttpStatus.OK.value()
        Mockito.verify(response).contentType = MediaType.APPLICATION_JSON_VALUE
        Mockito.verify(response).characterEncoding = StandardCharsets.UTF_8.name()
    }

    @Test
    fun testWriteSuccessResponseWithoutData() {
        JsonResponseWriter.writeSuccessResponse(response, jsonMapper)
        printWriter.flush()
        val result = stringWriter.toString()

        assertTrue(result.contains("\"data\":null"))
        Mockito.verify(response).status = HttpStatus.OK.value()
    }

    @Test
    fun testWriteFailedResponse() {
        val errorCode: ErrorResultCode = DefaultErrorResultCode.SERVER_ERROR

        JsonResponseWriter.writeFailedResponse(
            response,
            jsonMapper,
            HttpStatus.BAD_REQUEST,
            errorCode,
            "错误信息",
        )
        printWriter.flush()
        val result = stringWriter.toString()

        assertTrue(result.contains("500"))
        assertTrue(result.contains("错误信息"))
        Mockito.verify(response).status = HttpStatus.BAD_REQUEST.value()
    }

    @Test
    fun testWriteSuccessResponseIOException() {
        val errorResponse = Mockito.mock(HttpServletResponse::class.java)
        Mockito.`when`(errorResponse.writer).thenThrow(IOException("mock error"))

        assertThrows(RuntimeException::class.java) {
            JsonResponseWriter.writeSuccessResponse(errorResponse, jsonMapper, "fail")
        }
    }
}
