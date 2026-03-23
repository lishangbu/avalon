package io.github.lishangbu.avalon.web.response

import io.github.lishangbu.avalon.web.result.ApiResult
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import tools.jackson.databind.json.JsonMapper

/**
 * API 响应包装增强器
 *
 * 统一将控制器返回值包装为 [ApiResult]
 *
 * @author lishangbu
 * @since 2023/5/1
 */
@RestControllerAdvice(basePackages = ["io.github.lishangbu.avalon"])
class ApiResultResponseAdvice(
    /** JSON 映射器 */
    private val jsonMapper: JsonMapper,
) : ResponseBodyAdvice<Any> {
    /** 对所有控制器返回值启用统一包装 */
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = true

    /** 在写入响应体前将返回值转换为统一响应结构 */
    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? = wrapApiResult(body)

    /** 将普通返回值包装为 [ApiResult]，字符串返回值额外序列化为 JSON 文本 */
    private fun wrapApiResult(body: Any?): Any =
        when (body) {
            is String -> jsonMapper.writeValueAsString(ApiResult.ok(body))
            is ApiResult<*> -> body
            else -> ApiResult.ok(body)
        }
}
