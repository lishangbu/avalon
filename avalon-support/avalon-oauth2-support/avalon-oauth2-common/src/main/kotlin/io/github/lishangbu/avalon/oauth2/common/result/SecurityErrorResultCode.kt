package io.github.lishangbu.avalon.oauth2.common.result

import io.github.lishangbu.avalon.web.result.ErrorResultCode

/**
 * 安全模块默认错误码
 *
 * 定义认证与授权场景下的通用错误码
 */
enum class SecurityErrorResultCode(
    /** 状态码值 */
    private val codeValue: Int,
    /** 消息值 */
    private val messageValue: String,
) : ErrorResultCode {
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    ;

    /** 获取状态码 */
    override fun code(): Int = codeValue

    /** 获取错误信息 */
    override fun errorMessage(): String = messageValue
}
