package io.github.lishangbu.avalon.web.result

/**
 * web api 结果默认错误码 定义常见的 HTTP 错误码与默认错误信息
 *
 * @author lishangbu
 * @since 2025/4/8 参数错误 资源不存在 方法不支持 服务器内部错误
 */

/** Web API 默认错误码。 */
enum class DefaultErrorResultCode(
    private val value: Int,
    private val message: String,
) : ErrorResultCode {
    BAD_REQUEST(400, "Bad Request"),
    RESOURCE_NOT_FOUND(404, "Resource Not Found"),
    METHOD_NOT_ALLOWED(405, "METHOD NOT ALLOWED"),
    SERVER_ERROR(500, "Internal Server Error"),
    ;

    override fun code(): Int = value

    override fun errorMessage(): String = message
}
