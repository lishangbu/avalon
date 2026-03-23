package io.github.lishangbu.avalon.web.result

/**
 * Web API 默认错误码
 *
 * 定义常见 HTTP 错误对应的默认业务码
 */
enum class DefaultErrorResultCode(
    /** 值 */
    private val value: Int,
    /** 消息 */
    private val message: String,
) : ErrorResultCode {
    BAD_REQUEST(400, "Bad Request"),
    RESOURCE_NOT_FOUND(404, "Resource Not Found"),
    METHOD_NOT_ALLOWED(405, "METHOD NOT ALLOWED"),
    SERVER_ERROR(500, "Internal Server Error"),
    ;

    /** 获取错误码 */
    override fun code(): Int = value

    /** 获取错误消息 */
    override fun errorMessage(): String = message
}
