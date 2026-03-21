package io.github.lishangbu.avalon.authorization.model

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty

/**
 * 邮箱验证码请求
 *
 * @param email 邮箱
 * @author lishangbu
 * @since 2026/3/13
 */
data class EmailCodeRequest(
    @field:NotEmpty(message = "邮箱不能为空") @field:Email(message = "邮箱不合法") val email: String,
)
