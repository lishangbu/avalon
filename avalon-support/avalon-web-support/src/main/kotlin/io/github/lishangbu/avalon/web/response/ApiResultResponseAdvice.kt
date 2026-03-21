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
 * API 响应结果包装增强器，统一封装所有 Controller 返回值为 ApiResult 支持自动包装普通对象、字符串类型和已包装的 ApiResult
 * 类型，字符串类型特殊处理，保证响应内容为标准 JSON 适用于 io.github.lishangbu.avalon 包下所有 RestController
 *
 * @param body 原始响应体
 * @param returnType 方法返回类型参数
 * @param selectedContentType 响应内容类型
 * @param selectedConverterType 消息转换器类型
 * @param request 当前请求对象
 * @param response 当前响应对象
 * @param body 原始响应体
 * @return 包装后的响应体 包装 API 调用结果
 * @return 包装后的 ApiResult 或 JSON 字符串
 * @author lishangbu
 * @since 2023/5/1 判断是否需要处理响应体（本实现总是处理） 响应体写出前的统一包装处理
 */

/** API 响应结果包装增强器。 */
@RestControllerAdvice(basePackages = ["io.github.lishangbu.avalon"])
class ApiResultResponseAdvice(
    private val jsonMapper: JsonMapper,
) : ResponseBodyAdvice<Any> {
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? = wrapApiResult(body)

    private fun wrapApiResult(body: Any?): Any =
        when (body) {
            is String -> jsonMapper.writeValueAsString(ApiResult.ok(body))
            is ApiResult<*> -> body
            else -> ApiResult.ok(body)
        }
}
