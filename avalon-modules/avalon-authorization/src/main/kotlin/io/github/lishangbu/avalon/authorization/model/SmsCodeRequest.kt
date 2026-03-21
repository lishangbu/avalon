package io.github.lishangbu.avalon.authorization.model

import jakarta.validation.constraints.NotEmpty

/**
 * 短信验证码请求
 *
 * @param phone 手机号
 * @author lishangbu
 * @since 2026/3/13
 */
data class SmsCodeRequest(
    @field:NotEmpty(message = "手机号不能为空") val phone: String,
)
