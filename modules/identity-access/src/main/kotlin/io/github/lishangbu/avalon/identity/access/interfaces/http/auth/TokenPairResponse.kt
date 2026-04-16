package io.github.lishangbu.avalon.identity.access.interfaces.http.auth

import io.github.lishangbu.avalon.identity.access.domain.authentication.AuthenticatedTokenPair

/**
 * 认证成功后返回给客户端的 token 对。
 *
 * 该响应同时暴露 access token、refresh token 及它们的过期时间，
 * 方便客户端统一更新本地会话状态。
 *
 * @property accessToken 用于访问受保护资源的短时效 JWT。
 * @property accessTokenExpiresAt access token 的绝对过期时间。
 * @property refreshToken 用于换取下一枚 access token 的 opaque token。
 * @property refreshTokenExpiresAt refresh token 的绝对过期时间。
 * @property sessionId 本次会话的稳定标识，可用于会话管理接口。
 */
data class TokenPairResponse(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
    val sessionId: String,
)

/**
 * 把认证产出的 token 对转换为 HTTP 响应。
 *
 * @return 可直接返回给客户端的认证成功响应。
 */
fun AuthenticatedTokenPair.toResponse(): TokenPairResponse =
    TokenPairResponse(
        accessToken = accessToken,
        accessTokenExpiresAt = accessTokenExpiresAt.toString(),
        refreshToken = refreshToken,
        refreshTokenExpiresAt = refreshTokenExpiresAt.toString(),
        sessionId = sessionId,
    )