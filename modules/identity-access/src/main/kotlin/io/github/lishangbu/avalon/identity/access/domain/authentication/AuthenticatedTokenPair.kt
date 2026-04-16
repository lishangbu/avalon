package io.github.lishangbu.avalon.identity.access.domain.authentication

import java.time.Instant

/**
 * 认证成功后返回给客户端的一对令牌。
 *
 * @property accessToken access token 文本。
 * @property accessTokenExpiresAt access token 过期时间。
 * @property refreshToken refresh token 文本。
 * @property refreshTokenExpiresAt refresh token 过期时间。
 * @property sessionId 会话标识。
 */
data class AuthenticatedTokenPair(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val sessionId: String,
)