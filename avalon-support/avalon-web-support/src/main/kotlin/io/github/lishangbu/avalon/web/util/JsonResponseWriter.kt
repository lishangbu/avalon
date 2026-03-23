package io.github.lishangbu.avalon.web.util

import io.github.lishangbu.avalon.web.result.ApiResult
import io.github.lishangbu.avalon.web.result.ErrorResultCode
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * JSON 响应写入工具
 *
 * 统一写入成功和失败响应，并设置字符编码、内容类型与 HTTP 状态码
 *
 * @author lishangbu
 * @since 2025/8/23
 */
object JsonResponseWriter {
    /** 写入成功响应 */
    @JvmStatic
    fun writeSuccessResponse(
        response: HttpServletResponse,
        jsonMapper: JsonMapper,
        data: Any?,
    ) {
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpStatus.OK.value()
        try {
            response.writer.use { writer ->
                writer.write(jsonMapper.writeValueAsString(ApiResult.ok(data)))
                writer.flush()
            }
        } catch (ex: IOException) {
            log.error("写入response失败", ex)
            throw RuntimeException(ex)
        }
    }

    /** 写入成功响应 */
    @JvmStatic
    fun writeSuccessResponse(
        response: HttpServletResponse,
        jsonMapper: JsonMapper,
    ) {
        writeSuccessResponse(response, jsonMapper, null)
    }

    /** 写入失败响应 */
    @JvmStatic
    fun writeFailedResponse(
        response: HttpServletResponse,
        jsonMapper: JsonMapper,
        httpStatus: HttpStatus,
        errorResultCode: ErrorResultCode,
        vararg errorMessages: String,
    ) {
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = httpStatus.value()
        try {
            response.writer.use { writer ->
                writer.write(
                    jsonMapper.writeValueAsString(ApiResult.failed(errorResultCode, *errorMessages)),
                )
                writer.flush()
            }
        } catch (ex: IOException) {
            log.error("写入response失败", ex)
            throw RuntimeException(ex)
        }
    }

    /** 日志记录器 */
    private val log = LoggerFactory.getLogger(JsonResponseWriter::class.java)
}
