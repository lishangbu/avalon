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
import java.util.*

/**
 * JSON 格式的 response 写入工具类 提供写入成功与失败响应的便捷方法，自动设置字符编码/Content-Type/状态码
 *
 * @author lishangbu
 * @since 2025/8/23
 */

/** JSON 格式的 response 写入工具类。 */
object JsonResponseWriter {
    @JvmStatic
    fun writeSuccessResponse(
        response: HttpServletResponse,
        jsonMapper: JsonMapper,
        data: Any?,
    ) {
        Objects.requireNonNull(jsonMapper, "jsonMapper")
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

    @JvmStatic
    fun writeSuccessResponse(
        response: HttpServletResponse,
        jsonMapper: JsonMapper,
    ) {
        writeSuccessResponse(response, jsonMapper, null)
    }

    @JvmStatic
    fun writeFailedResponse(
        response: HttpServletResponse,
        jsonMapper: JsonMapper,
        httpStatus: HttpStatus,
        errorResultCode: ErrorResultCode,
        vararg errorMessages: String,
    ) {
        Objects.requireNonNull(jsonMapper, "jsonMapper")
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

    private val log = LoggerFactory.getLogger(JsonResponseWriter::class.java)
}
