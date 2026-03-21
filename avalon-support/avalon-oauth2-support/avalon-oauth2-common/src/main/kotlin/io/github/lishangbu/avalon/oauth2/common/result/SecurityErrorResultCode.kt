package io.github.lishangbu.avalon.oauth2.common.result

import io.github.lishangbu.avalon.web.result.ErrorResultCode

/**
 * 安全模块使用的默认错误码 提供常见的 HTTP 授权/认证相关错误码定义
 *
 * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1: Authentication</a> 403
 *   Forbidden
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1: Semantics and
 *   Content</a> Content</a>
 * @author lishangbu
 * @since 2025/4/8 401 Unauthorized
 */
enum class SecurityErrorResultCode(
    private val codeValue: Int,
    private val messageValue: String,
) : ErrorResultCode {
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    ;

    override fun code(): Int = codeValue

    override fun errorMessage(): String = messageValue
}
