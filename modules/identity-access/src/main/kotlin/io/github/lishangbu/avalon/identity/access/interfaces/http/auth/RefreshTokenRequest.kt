package io.github.lishangbu.avalon.identity.access.interfaces.http.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 刷新 access token 时提交的请求体。
 *
 * @property refreshToken 客户端当前持有的 refresh token 明文。
 */
data class RefreshTokenRequest(
    @field:NotBlank
    @field:Size(max = 512)
    val refreshToken: String,
)

/**
 * 提取可直接交给认证服务的 refresh token 明文。
 *
 * 映射时统一裁剪首尾空白字符，避免 Resource 和应用服务重复处理表现层噪音。
 *
 * @return 标准化后的 refresh token。
 */
fun RefreshTokenRequest.toRefreshToken(): String =
    refreshToken.trim()